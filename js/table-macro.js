/**
 * Extends the AsciiDoc syntax to support a table macro. It can create an asciidoc table from the sql query provided in its parameters. Sometimes default query is exectuted which needs to be passed via asciidoc attributes
 *
 * Usage:
 *
 * table::MEMBER[query="ddl", columnDataStyle="c,c,p,c,p"]
 *
 * table::batch_job_type[query="select attribute_name as Name, attribute_value as Value from config order by attribute_name", columnDataStyle="c,c,p"]
 *
 * @author Prashant C Chaturvedi <pccfrmvns@gmail.com>
 */
'use strict'
var cp = require('child_process');
const Opal = global.Opal;
const DEBUG = false;
function tableMacro (macroName, context) {
    return function(){
        const self = this;
        self.named('table');
        self.process((parent, target, attrs) => {
            let query = attrs.query;
            if(query.length <= 32){
                var asciidocAttributes = parent.getDocument().getAttributes();
                query = asciidocAttributes[query];
                query = query.replace("?", '\''+target+'\'');
            }
            execute(query, this, parent, attrs, target);
        });
    }
}
function execute(query, processor, parent, attrs, table){
    var result = cp.execSync(`${process.argv[0]} "${__dirname}/db-query.js" "${query}"`, { encoding: 'utf8', maxBuffer: 1000 * 1024 * 1024 });
    try{
        var output = JSON.parse(result);
        DEBUG && console.log(output);
        createTable(processor, parent, attrs, table, output);
    }catch (err) {
        console.log(err)
    }
}

function createTable(processor, parent, attrs, target, result){
    const createHtmlFragment = (html) => processor.createBlock(parent, 'pass', html, attrs);
    
    var caption = getCaption(parent, attrs, target);
    const nodes = [];
    nodes.push(createHtmlFragment('<table id='+target+' class="tableblock frame-all grid-all stretch">'));
    nodes.push(createHtmlFragment('<caption class="title">' + caption + '</caption>'));
    nodes.push(createHtmlFragment('<colgroup>'));
    nodes.push(createHtmlFragment('<col style="width: 100%;">'));
    nodes.push(createHtmlFragment('</colgroup>'));
    nodes.push(createHtmlFragment('<thead>'));
    nodes.push(createHtmlFragment('<tr>'));
    var maxCols = 0;
    if (result.metaData && result.metaData.length) {
        result.metaData.forEach(function (column, index) {
            nodes.push(createHtmlFragment('<th class="tableblock halign-left valign-top">'));
            nodes.push(createHtmlFragment(column.name));
            nodes.push(createHtmlFragment("</th>"));
            maxCols = maxCols + 1;
        });
    }
    nodes.push(createHtmlFragment('</tr>'));
    nodes.push(createHtmlFragment('</thead>'));
    nodes.push(createHtmlFragment('<tbody>'));
    //attrs: { query: 'ddl', columnDataStyle: 'c,c,p,c,p' }
    var columnStyles = attrs.columnDataStyle;
    var colStyleArray = new Array();
    if(columnStyles){
        colStyleArray = columnStyles.split(",");
    }
    if (result.rows && result.rows.length) {
        result.rows.forEach(function (row, index) {
            nodes.push(createHtmlFragment('<tr>'));
            for (var i = 0; i < result.metaData.length; i++) {
                nodes.push(createHtmlFragment('<td class="tableblock halign-left valign-top">'));
                nodes.push(createHtmlFragment('<p class="tableblock">'));
                if(colStyleArray[i] && colStyleArray[i].trim() == 'c'){
                    nodes.push(createHtmlFragment('<code>'));
                }
                nodes.push(createHtmlFragment(`${row[i]}`));
                if(colStyleArray[i] && colStyleArray[i].trim() == 'c'){
                    nodes.push(createHtmlFragment('<\code>'));
                }
                nodes.push(createHtmlFragment('</p>'));
                nodes.push(createHtmlFragment('</td>'));
            }
            nodes.push(createHtmlFragment("</tr>"));
        });
    }
    nodes.push(createHtmlFragment('<tbody>'));
    nodes.push(createHtmlFragment('</table>'));
    parent.blocks.push(...nodes);
}

function getCaption(parent, attrs, target){
    var captionBuilder = [];
    captionBuilder.push('Table: <code>');
    captionBuilder.push(target);
    captionBuilder.push("</code>");
    return captionBuilder.join("");
}

module.exports.register = function register (registry, context = {}) {
    if (typeof registry.register === 'function') {
        registry.register(function () {
            this.blockMacro(tableMacro('table', context));
    });
    } else if (typeof registry.block === 'function') {
        registry.blockMacro(tableMacro('table', context));
    }
}

//execute('select col.column_name as NAME, col.data_type as TYPE, col.nullable as NULLABLE, col.data_default as DEFAULT_VALUE, com.comments as COMMENTS from all_tab_columns col LEFT JOIN all_col_comments com ON col.column_name = com.column_name AND col.table_name=com.table_name and com.owner=col.owner where col.table_name = \'SSA_COMPOSITE_RESPONSE\'');