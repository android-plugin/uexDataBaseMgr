   var cText = 0;
   var cJson = 1;
   var cInt = 2;

   var db;


   function createDB() {
       var dbName = document.getElementById('name').value;
       db = uexDataBaseMgr.open(dbName);
       alert(db!=null);
   }

   function createTable() {
       //var sql = "CREATE TABLE testTable (_id  INTEGER PRIMARY KEY,name TEXT)";
       uexDataBaseMgr.sql(db,document.getElementById('createTable').value, function(isSuccess) {
           alert("createTable result:"+isSuccess);
       });
   }

   function insertData() {
       //	var sql = "INSERT INTO testTable (name) VALUES ('this is test')";
       uexDataBaseMgr.sql(db, document.getElementById('insertData').value, function(isSuccess) {
           alert("insertData result: "+isSuccess);
       });
   }

   function selectData() {
       //	var sql = "SELECT * FROM testTable";
       uexDataBaseMgr.select(db, document.getElementById('selectData').value, function(isSuccess,data) {
           if (!isSuccess) {
               alert("查询失败");
           } else {
               alert("select result:"+JSON.stringify(data));
           }
       });
   }

   function updateData() {
       //	var sql = UPDATE testTable SET name='这是更改' WHERE _id = 1;
       uexDataBaseMgr.sql(db, document.getElementById('updateData').value, function(isSuccess) {
           alert("updateData result:"+isSuccess);
       });
   }

   function transaction() {
       uexDataBaseMgr.transactionEx(db, inFunc, function(isSuccess) {
           alert("transaction result:"+isSuccess);
       });
   }


   function inFunc() {
       uexDataBaseMgr.sql(db, document.getElementById('transactiontData').value);
   }

   function closeDataBase() {
       var result = uexDataBaseMgr.close(db);
       alert("close result:"+result);
   }
