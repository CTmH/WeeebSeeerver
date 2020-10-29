import sqlite3
import os


class RecordDatabase():
    def __init__(self, dir=os.path.dirname(os.path.abspath(__file__))+'/database', filename='records.db'):
        os.makedirs(dir, exist_ok=True)
        self.dbPath = os.path.join(dir, filename)
        self.connect = None
        self.cursor = None

    def connect_and_init(self):
        self.connect = sqlite3.connect(self.dbPath)
        self.cursor = self.connect.cursor()
        self.cursor.execute('CREATE TABLE IF NOT EXISTS records(\
            STUDENT_ID INTEGER,\
            STUDENT_NAME TEXT,\
            CLASS_ID INTEGER\
        )')

    def insert_record(self,
                      stu_id: str,
                      stu_name: str,
                      class_id: int):
        assert self.cursor,'请先执行connect_and_init方法！'
        self.cursor.execute('INSERT INTO records VALUES(?,?,?)',
                                   (stu_id, stu_name, class_id))
        self.connect.commit()

    def select_records(self,
                      stu_id: str=None,
                      stu_name: str=None,
                      class_id: str=None):
        assert self.cursor,'请先执行connect_and_init方法！'
        filter=""
        if(stu_id):
            filter+=' STUDENT_ID='+str(stu_id)
        if(stu_name):
            filter+=' STUDENT_NAME='+str(stu_name)
        if(class_id):
            filter+=' CLASS_ID='+str(class_id)
        if(filter!=""):
            filter=" WHERE"+filter
        return self.cursor.execute('SELECT * FROM records'+filter)

    def __del__(self):
        if(self.cursor):
            self.cursor.close()
        if(self.connect):
            self.connect.close()


# if __name__ == "__main__":
r=RecordDatabase()
r.connect_and_init()
a=input()
print(str(list(r.select_records(a))))