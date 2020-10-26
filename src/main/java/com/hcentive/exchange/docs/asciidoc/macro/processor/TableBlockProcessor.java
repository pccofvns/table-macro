package com.hcentive.exchange.docs.asciidoc.macro.processor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.asciidoctor.ast.Cell;
import org.asciidoctor.ast.Column;
import org.asciidoctor.ast.Row;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.ast.Table;
import org.asciidoctor.extension.BlockMacroProcessor;

/**
 * @author Prashant C Chaturvedi
 *
 */
public class TableBlockProcessor extends BlockMacroProcessor {

	private static final String BLANK = String.valueOf("");

	public TableBlockProcessor(String macroName, Map<String, Object> config) {
		super(macroName, config);
	}
	
	private static Set<String> ignoredColumns = Stream.of("CREATED_BY", "CREATED_DATE", "UPDATED_BY", "UPDATED_DATE").collect(Collectors.toSet());
	
	Map<String, String> colFormattingMap = Stream.of(new String[][] {
		  { "m", "`" },
		  { "c", "`" },
		  { "p", "" },
		  { "b", "*" },
		  { "", "" },
		  { null, "" },
		  { "i", "_" },
		}).collect(Collectors.toMap(data -> data[0], data -> data[1]));

	@Override
	public Object process(StructuralNode parent, String target, Map<String, Object> attributes) {
		Map<String, Object> globalAttributes = parent.getDocument().getAttributes();
		Table table = null; 
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			String driver = (String) globalAttributes.get("driver");
			Class.forName(driver);
			String url = (String) globalAttributes.get("url");
			String username = (String) globalAttributes.get("username");
			String password = (String) globalAttributes.get("password");
			conn = DriverManager.getConnection(url,username,password);
			String sql = "ddl";
			String query = (String)attributes.get("query");
			if("ddl".equalsIgnoreCase(query)) {
				sql = (String) globalAttributes.get("ddl");
			}else {
				sql = query;
			}
			stmt = conn.prepareStatement(sql);
			if("ddl".equalsIgnoreCase(query)) {
				stmt.setString(1, target);
				stmt.setString(2, username);
			}
			rs = stmt.executeQuery();
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnCount = rsmd.getColumnCount();
			String[] colStyles = new String[columnCount];
			Arrays.asList(((String)attributes.get("columnDataStyle")).split(",")).stream().map(String::strip).collect(Collectors.toList()).toArray(colStyles);
			table = createTable(parent, target, globalAttributes, columnCount);
			
			String[][] headers = prepareTableHeader(table, rsmd, columnCount, colStyles);
			Set<String> uniqueColumns = new HashSet<>();
			uniqueColumns.addAll(ignoredColumns);
			while(rs.next()){
				prepareTableContent(table, rs, headers, uniqueColumns);
			}
			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}finally {
			try {
				rs.close();
				stmt.close();
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		
		return table;
	}

	private Table createTable(StructuralNode parent, String target, Map<String, Object> globalAttributes,
			int columnCount) {
		Table table;
		Map<String, Object> tableAttrs = new HashMap<>();
		tableAttrs.putIfAbsent("cols", columnCount);
		tableAttrs.putIfAbsent("options", "header");
		table = createTable(parent, tableAttrs);
		appendCaption(parent, target, globalAttributes, table);
		return table;
	}

	private String[][] prepareTableHeader(Table table, ResultSetMetaData rsmd, int columnCount, String[] colStyles) throws SQLException {
		Row headerRow = createTableRow(table);
		String[][] headers = new String[columnCount][2];
		for (int i = 0; i < columnCount; i++) {
			List<Cell> cells = headerRow.getCells();
			Column headerColumn = createTableColumn(table, i);
			String columnLabel = rsmd.getColumnLabel(i + 1);
			headers[i][0] = columnLabel;
			headers[i][1] = colStyles[i];
			Cell headerCell = createTableCell(headerColumn, "*"+columnLabel+"*");
			cells.add(headerCell);
		}
		table.getBody().add(headerRow);
		return headers;
	}

	private void prepareTableContent(Table table, ResultSet rs, String[][] headers, Set<String> uniqueColumns) throws SQLException {
		boolean rowExists = false;
		String firstCol = getFirstColumnName(rs, headers);
		rowExists = !uniqueColumns.add(firstCol);
		if(rowExists) {
			return;
		}
		Row dataRow = createTableRow(table);
		
		int i = 0;
		for (String[] header : headers) {
			Column columnNameColumn = createTableColumn(table, i);
			String markup = colFormattingMap.getOrDefault(header[1],"");
			String txt = rs.getString(header[0]);
			String text = txt != null? txt.strip(): BLANK;
			String columnName = text.isBlank()? BLANK: markup + text + markup;
			Cell columnNameCell = createTableCell(columnNameColumn, columnName);
			dataRow.getCells().add(columnNameCell);
			i++;
		}
		
		table.getBody().add(dataRow);
	}

	private String getFirstColumnName(ResultSet rs, String[][] headers) throws SQLException {
		String txt = rs.getString(headers[0][0]);
		String text = txt != null? txt.strip(): BLANK;
		return text;
	}

	private void appendCaption(StructuralNode parent, String target, Map<String, Object> globalAttributes,
			Table table) {
		StringBuilder captionBuilder = new StringBuilder(String.valueOf(globalAttributes.get("table-caption")));
		captionBuilder.append(" ");
		int counter = parent.getDocument().getAndIncrementCounter("table");
		captionBuilder.append(String.valueOf(counter));
		captionBuilder.append(". Table: `");
		captionBuilder.append(target);
		captionBuilder.append("`");
		table.setTitle(captionBuilder.toString());
	}

}
