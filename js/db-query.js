process.env.ORA_SDTZ = 'UTC';
const oracledb = require('oracledb');
oracledb.outFormat = oracledb.OUT_FORMAT_ARRAY;
oracledb.maxRows = 1000;
const dbConfig = require('./dbconfig.js');
const query = process.argv[2];
const connection = oracledb.getConnection(dbConfig);

function executeQuery(query){
    try {
        let sql, binds, options;
        sql = `${query}`;
        binds = [];
        connection.then((connected) =>{
            connected.execute(sql, binds).then((result) =>{
                //console.log(typeof result);
                console.log(JSON.stringify(result));
                closeConnection(connected);
            }).catch((err) =>{
                console.log(err)
            });
        });
    }catch (err) {
        console.error(err);
    }
}
function closeConnection(connection){
    if (connection) {
         try {
             connection.close();
         } catch (err) {
             connection.error(err);
         }
     } 
}
executeQuery(query);
//executeQuery('select col.column_name as NAME, col.data_type as TYPE, col.nullable as NULLABLE, col.data_default as DEFAULT_VALUE, com.comments as COMMENTS from all_tab_columns col LEFT JOIN all_col_comments com ON col.column_name = com.column_name AND col.table_name=com.table_name and com.owner=col.owner where col.table_name = \'ELG_MEMBER\'');