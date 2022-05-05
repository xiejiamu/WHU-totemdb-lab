package drz.oddb.Transaction;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import drz.oddb.Log.*;
import drz.oddb.Memory.*;


import drz.oddb.show.PrintResult;
import drz.oddb.show.ShowTable;
import drz.oddb.Transaction.SystemTable.*;

import drz.oddb.parse.*;

public class TransAction {
    public TransAction(Context context) {
        this.context = context;
        RedoRest();
    }

    Context context;
    public MemManage mem = new MemManage();

    public ObjectTable topt = mem.loadObjectTable();
    public ClassTable classt = mem.loadClassTable();
    public DeputyTable deputyt = mem.loadDeputyTable();
    public BiPointerTable biPointerT = mem.loadBiPointerTable();
    public SwitchingTable switchingT = mem.loadSwitchingTable();

    LogManage log = new LogManage(this);

    public void SaveAll( )
    {
        mem.saveObjectTable(topt);
        mem.saveClassTable(classt);
        mem.saveDeputyTable(deputyt);
        mem.saveBiPointerTable(biPointerT);
        mem.saveSwitchingTable(switchingT);
        mem.saveLog(log.LogT);
        while(!mem.flush());
        while(!mem.setLogCheck(log.LogT.logID));
        mem.setCheckPoint(log.LogT.logID);//成功退出,所以新的事务块一定全部执行
    }

    public void Test(){
        TupleList tpl = new TupleList();
        Tuple t1 = new Tuple();
        t1.tupleHeader = 5;
        t1.tuple = new Object[t1.tupleHeader];
        t1.tuple[0] = "a";
        t1.tuple[1] = 1;
        t1.tuple[2] = "b";
        t1.tuple[3] = 3;
        t1.tuple[4] = "e";
        Tuple t2 = new Tuple();
        t2.tupleHeader = 5;
        t2.tuple = new Object[t2.tupleHeader];
        t2.tuple[0] = "d";
        t2.tuple[1] = 2;
        t2.tuple[2] = "e";
        t2.tuple[3] = 2;
        t2.tuple[4] = "v";

        tpl.addTuple(t1);
        tpl.addTuple(t2);
        String[] attrname = {"attr2","attr1","attr3","attr5","attr4"};
        int[] attrid = {1,0,2,4,3};
        String[]attrtype = {"int","char","char","char","int"};

        PrintSelectResult(tpl,attrname,attrid,attrtype);

        int[] a = InsertTuple(t1);
        Tuple t3 = GetTuple(a[0],a[1]);
        int[] b = InsertTuple(t2);
        Tuple t4 = GetTuple(b[0],b[1]);
        System.out.println(t3);
    }

    private boolean RedoRest(){//redo
        LogTable redo;
        if((redo=log.GetReDo())!=null) {
            int redonum = redo.logTable.size();   //先把redo指令加前面
            for (int i = 0; i < redonum; i++) {
                String s = redo.logTable.get(i).str;

                log.WriteLog(s);
                query(s, true);
            }
        }else{
            return false;
        }
        return true;
    }

    public String query(String s, boolean show) {

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(s.getBytes());
        parse p = new parse(byteArrayInputStream);
        try {
            String[] aa = p.Run();

            switch (Integer.parseInt(aa[0])) {
                case parse.OPT_CREATE_ORIGINCLASS:
                    log.WriteLog(s);
                    CreateOriginClass(aa);
                    if (show) new AlertDialog.Builder(context).setTitle("提示").setMessage("创建成功").setPositiveButton("确定",null).show();
                    break;
                case parse.OPT_CREATE_SELECTDEPUTY:
                    log.WriteLog(s);
                    CreateSelectDeputy(aa);
                    if (show) new AlertDialog.Builder(context).setTitle("提示").setMessage("创建成功").setPositiveButton("确定",null).show();
                    break;
                case parse.OPT_DROP:
                    log.WriteLog(s);
                    Drop(aa);
                    if (show) new AlertDialog.Builder(context).setTitle("提示").setMessage("删除成功").setPositiveButton("确定",null).show();
                    break;
                case parse.OPT_INSERT:
                    log.WriteLog(s);
                    Insert(aa);
                    if (show) new AlertDialog.Builder(context).setTitle("提示").setMessage("插入成功").setPositiveButton("确定",null).show();
                    break;
                case parse.OPT_DELETE:
                    log.WriteLog(s);
                    Delete(aa);
                    if (show) new AlertDialog.Builder(context).setTitle("提示").setMessage("删除成功").setPositiveButton("确定",null).show();
                    break;
                case parse.OPT_SELECT_DERECTSELECT:
                    DirectSelect(aa, true);
                    break;
                case parse.OPT_SELECT_INDERECTSELECT:
                    InDirectSelect(aa, true);
                    break;
                case parse.OPT_CREATE_UPDATE:
                    log.WriteLog(s);
                    Update(aa);
                    if (show) new AlertDialog.Builder(context).setTitle("提示").setMessage("更新成功").setPositiveButton("确定",null).show();
                    break;
                case parse.OPT_UNION:
                    UnionOp(aa, true);
                    break;
                case parse.OPT_CREATE_UNIONDEPUTY:
                    log.WriteLog(s);
                    CreateUnionDeputy(aa);
                    if (show) new AlertDialog.Builder(context).setTitle("提示")
                            .setMessage("创建成功").setPositiveButton("确定",null).show();
                default:
                    break;

            }
        } catch (ParseException e) {

            e.printStackTrace();
        }

        return s;

    }

    // Edited by Xukeyizhi
    // 9,6,3,UNION,6,3,n1,0,0,names,birth,0,0,births,s1,0,0,salarys,nandb,n1,=,"gg",n1,0,0,names,birth,0,0,births,s1,0,0,salarys,nandb,n1,=,"aa"
    // 0 1 2 3     4 5 6  7 8 9     10    111213     14 151617      18    19 2021

    // 9,6,3,UNION,9,6,3,UNION,6,3,n1,0,0,names,birth,0,0,births,s1,0,0,salarys,nandb,n1,=,"gg",n1,0,0,names,birth,0,0,births,s1,0,0,salarys,nandb,n1,=,"aa",n1,0,0,names,birth,0,0,births,s1,0,0,salarys,nandb,n1,=,"bb"
    // 0 1 2 3     4 5 6 7     8 9 10 111213    14    151617     18 192021      22    23 2425
    private TupleList UnionOp(String[] p, boolean show) {
        int union_num = 0;
        for (int i = 0; i < p.length; i++) {
            if (p[i].equals("UNION")) {
                union_num++;
                System.out.println("UNION detected: at " + i);
            }
        }
        int last_u = 4 * union_num - 1;
        int attrnumber = Integer.parseInt(p[2]);
        int sel_start = last_u + 3;
        int sel_end = last_u + attrnumber * 4 + 6;
        int[] attrid = new int[attrnumber];

        // the 1st select
        ArrayList<String> l1 = new ArrayList<String>();
        l1.add(p[1]);
        l1.add(p[2]);
        for (int i = sel_start; i <= sel_end; i++) {
            l1.add(p[i]);
        }
        String[] s1 = new String[l1.size()];
        l1.toArray(s1);
        TupleList tpl1 = new TupleList();
        TupleList tpl2 = new TupleList();
        if (Integer.parseInt(s1[0]) == parse.OPT_SELECT_DERECTSELECT) {
            tpl1 = DirectSelect(s1, false);
        } else {
            tpl1 = InDirectSelect(s1, false);
        }

        String[] attrname = new String[attrnumber];
        String[] attrtype = new String[attrnumber];
        String classname1 = s1[2+4*attrnumber];
        int classid = 0;
        for(int i = 0;i < attrnumber;i++){
            for (ClassTableItem item:classt.classTable) {
                if (item.classname.equals(classname1) && item.attrname.equals(s1[2+4*i])) {
                    classid = item.classid;
                    attrid[i] = item.attrid;
                    attrtype[i] = item.attrtype;
                    attrname[i] = s1[5+4*i];
                    break;
                }
            }
        }

        // the 2nd select
        ArrayList<String> l2 = new ArrayList<String>();
        for (int i = 4; i < sel_start; i++)
            l2.add(p[i]);
        for (int i = sel_end + 1; i < p.length; i++)
            l2.add(p[i]);
        String[] s2 = new String[l2.size()];
        l2.toArray(s2);
        if (union_num == 1) {
            if (Integer.parseInt(s2[0]) == parse.OPT_SELECT_DERECTSELECT) {
                tpl2 = DirectSelect(s2, false);
            } else {
                tpl2 = InDirectSelect(s2, false);
            }
        }
        for(int i = 0; i < attrnumber; i++){
            attrid[i] = i;
        }

        tpl1.joinTupleUnion(tpl2);
        if (show) PrintSelectResult(tpl1,attrname,attrid,attrtype);
        return tpl1;
    }


    //CREATE CLASS dZ123 (nB1 int,nB2 char) ;
    //1,2,dZ123,nB1,int,nB2,char
    private void CreateOriginClass(String[] p) {
        String classname = p[2];
        int count = Integer.parseInt(p[1]);
        classt.maxid++;
        int classid = classt.maxid;
        for (int i = 0; i < count; i++) {
            classt.classTable.add(new ClassTableItem(classname, classid, count,i,p[2 * i + 3], p[2 * i + 4],"ori"));
        }
    }

    //INSERT INTO aa VALUES (1,2,"3");
    //4,3,aa,1,2,"3"
    //0 1 2  3 4  5
    private int Insert(String[] p){


        int count = Integer.parseInt(p[1]);
        for(int o =0;o<count+3;o++){
            p[o] = p[o].replace("\"","");
        }

        String classname = p[2];
        Object[] tuple_ = new Object[count];

        int classid = 0;

        for(ClassTableItem item:classt.classTable)
        {
            if(item.classname.equals(classname)){
                classid = item.classid;
            }
        }

        for(int j = 0;j<count;j++){
            tuple_[j] = p[j+3];
        }

        Tuple tuple = new Tuple(tuple_);
        tuple.tupleHeader=count;

        int[] a = InsertTuple(tuple);
        topt.maxTupleId++;
        int tupleid = topt.maxTupleId;
        topt.objectTable.add(new ObjectTableItem(classid,tupleid,a[0],a[1]));

        //向代理类加元组

        for(DeputyTableItem item:deputyt.deputyTable){
            if(classid == item.originid){
                //判断代理规则

                String attrtype=null;
                int attrid=0;
                for(ClassTableItem item1:classt.classTable){
                    if(item1.classid == classid&&item1.attrname.equals(item.deputyrule[0])) {
                        attrtype = item1.attrtype;
                        attrid = item1.attrid;
                        break;
                    }
                }

                if(Condition(attrtype,tuple,attrid,item.deputyrule[2])){
                    String[] ss= p.clone();
                    String s1 = null;

                    for(ClassTableItem item2:classt.classTable){
                        if(item2.classid == item.deputyid) {
                            s1 = item2.classname;
                            break;
                        }
                    }
                    //是否要插switch的值
                    //收集源类属性名
                    String[] attrname1 = new String[count];
                    int[] attrid1 = new int[count];
                    int k=0;
                    for(ClassTableItem item3 : classt.classTable){
                        if(item3.classid == classid){
                            attrname1[k] = item3.attrname;
                            attrid1[k] = item3.attrid;
                            k++;

                            if (k ==count)
                                    break;
                        }
                    }
                    for (int l = 0;l<count;l++) {
                        for (SwitchingTableItem item4 : switchingT.switchingTable) {
                            if (item4.attr.equals(attrname1[l])){
                                //判断被置换的属性是否是代理类的

                                for(ClassTableItem item8: classt.classTable){
                                    if(item8.attrname.equals(item4.deputy)&&Integer.parseInt(item4.rule)!=0){
                                        if(item8.classid==item.deputyid){
                                            int sw = Integer.parseInt(p[3+attrid1[l]]);
                                            ss[3+attrid1[l]] = new Integer(sw+Integer.parseInt(item4.rule)).toString();
                                            break;
                                        }
                                    }
                                }


                            }
                        }
                    }

                    ss[2] = s1;
                    int deojid=Insert(ss);
                    //插入Bi
                    biPointerT.biPointerTable.add(new BiPointerTableItem(classid,tupleid,item.deputyid,deojid));



                }
            }
        }
        return tupleid;



    }

    private boolean Condition(String attrtype,Tuple tuple,int attrid,String value1){
        String value = value1.replace("\"","");
        switch (attrtype){
            case "int":
                int value_int = Integer.parseInt(value);
                if(Integer.parseInt((String)tuple.tuple[attrid])==value_int)
                    return true;
                break;
            case "char":
                String value_string = value;
                if(tuple.tuple[attrid].equals(value_string))
                    return true;
                break;
            case "float":
                Float value_float = Float.parseFloat(value);
                if(tuple.tuple[attrid].equals(value_float))
                    return true;
                break;

        }
        return false;
    }
    //DELETE FROM bb WHERE t4="5SS";
    //5,bb,t4,=,"5SS"
    private void Delete(String[] p) {
        String classname = p[1];
        String attrname = p[2];
        int classid = 0;
        int attrid=0;
        String attrtype=null;
        for (ClassTableItem item:classt.classTable) {
            if (item.classname.equals(classname) && item.attrname.equals(attrname)) {
                classid = item.classid;
                attrid = item.attrid;
                attrtype = item.attrtype;
                break;
            }
        }
        //寻找需要删除的
        OandB ob2 = new OandB();
        for (Iterator it1 = topt.objectTable.iterator(); it1.hasNext();){
            ObjectTableItem item = (ObjectTableItem)it1.next();
            if(item.classid == classid){
                Tuple tuple = GetTuple(item.blockid,item.offset);
                if(Condition(attrtype,tuple,attrid,p[4])){
                    //需要删除的元组
                     OandB ob =new OandB(DeletebyID(item.tupleid));
                    for(ObjectTableItem obj:ob.o){
                        ob2.o.add(obj);
                    }
                    for(BiPointerTableItem bip:ob.b){
                        ob2.b.add(bip);
                    }

                }
            }
        }
        for(ObjectTableItem obj:ob2.o){
            topt.objectTable.remove(obj);
        }
        for(BiPointerTableItem bip:ob2.b) {
            biPointerT.biPointerTable.remove(bip);
        }

    }

    private OandB DeletebyID(int id){

        List<ObjectTableItem> todelete1 = new ArrayList<>();
        List<BiPointerTableItem>todelete2 = new ArrayList<>();
        OandB ob = new OandB(todelete1,todelete2);
        for (Iterator it1 = topt.objectTable.iterator(); it1.hasNext();){
            ObjectTableItem item  = (ObjectTableItem)it1.next();
            if(item.tupleid == id){
                //需要删除的tuple


                //删除代理类的元组
                int deobid = 0;

                for(Iterator it = biPointerT.biPointerTable.iterator(); it.hasNext();){
                    BiPointerTableItem item1 =(BiPointerTableItem) it.next();
                    if(item.tupleid == item1.deputyobjectid){
                        //it.remove();
                      if(!todelete2.contains(item1))
                            todelete2.add(item1);
                    }
                    if(item.tupleid == item1.objectid){
                        deobid = item1.deputyobjectid;
                        OandB ob2=new OandB(DeletebyID(deobid));

                        for(ObjectTableItem obj:ob2.o){
                            if(!todelete1.contains(obj))
                                todelete1.add(obj);
                        }
                        for(BiPointerTableItem bip:ob2.b){
                            if(!todelete2.contains(bip))
                                todelete2.add(bip);
                        }

                        //biPointerT.biPointerTable.remove(item1);

                    }
                }


                //删除自身
                DeleteTuple(item.blockid,item.offset);
                if(!todelete2.contains(item));
                    todelete1.add(item);





                }
            }

            return ob;
    }

    //DROP CLASS asd;
    //3,asd

    private void Drop(String[]p){
        List<DeputyTableItem> dti;
        dti = Drop1(p);
        for(DeputyTableItem item:dti){
            deputyt.deputyTable.remove(item);
        }
    }

    private List<DeputyTableItem> Drop1(String[] p){
        String classname = p[1];
        int classid = 0;
        //找到classid顺便 清除类表和switch表
        for (Iterator it1 = classt.classTable.iterator(); it1.hasNext();) {
            ClassTableItem item =(ClassTableItem) it1.next();
            if (item.classname.equals(classname) ){
                classid = item.classid;
                for(Iterator it = switchingT.switchingTable.iterator(); it.hasNext();) {
                    SwitchingTableItem item2 =(SwitchingTableItem) it.next();
                    if (item2.attr.equals( item.attrname)||item2.deputy .equals( item.attrname)){
                       it.remove();
                    }
                }
                it1.remove();
            }
        }
        //清元组表同时清了bi
        OandB ob2 = new OandB();
        for(ObjectTableItem item1:topt.objectTable){
            if(item1.classid == classid){
                OandB ob = DeletebyID(item1.tupleid);
                for(ObjectTableItem obj:ob.o){
                    ob2.o.add(obj);
                }
                for(BiPointerTableItem bip:ob.b){
                    ob2.b.add(bip);
                }
            }
        }
        for(ObjectTableItem obj:ob2.o){
            topt.objectTable.remove(obj);
        }
        for(BiPointerTableItem bip:ob2.b) {
            biPointerT.biPointerTable.remove(bip);
        }

        //清deputy
        List<DeputyTableItem> dti = new ArrayList<>();
        for(DeputyTableItem item3:deputyt.deputyTable){
            if(item3.deputyid == classid){
                if(!dti.contains(item3))
                    dti.add(item3);
            }
            if(item3.originid == classid){
                //删除代理类
                String[]s = p.clone();
                List<String> sname = new ArrayList<>();
                for(ClassTableItem item5: classt.classTable) {
                    if (item5.classid == item3.deputyid) {
                        sname.add(item5.classname);
                    }
                }
                for(String item4: sname){

                        s[1] = item4;
                        List<DeputyTableItem> dti2 = Drop1(s);
                        for(DeputyTableItem item8:dti2){
                            if(!dti.contains(item8))
                                dti.add(item8);
                        }

                }
                if(!dti.contains(item3))
                    dti.add(item3);
            }
        }
        return dti;

    }


    //SELECT  b1+2 AS c1,b2 AS c2,b3 AS c3 FROM  bb WHERE t1="1";
    //6,3,b1,1,2,c1,b2,0,0,c2,b3,0,0,c3,bb,t1,=,"1"
    //0 1 2  3 4 5  6  7 8 9  10 111213 14 15 16 17
    private TupleList DirectSelect(String[] p, boolean show){
        TupleList tpl = new TupleList();
        int attrnumber = Integer.parseInt(p[1]);
        String[] attrname = new String[attrnumber];
        int[] attrid = new int[attrnumber];
        String[] attrtype= new String[attrnumber];
        String classname = p[2+4*attrnumber];
        int classid = 0;
        for(int i = 0;i < attrnumber;i++){
            for (ClassTableItem item:classt.classTable) {
                if (item.classname.equals(classname) && item.attrname.equals(p[2+4*i])) {
                    classid = item.classid;
                    attrid[i] = item.attrid;
                    attrtype[i] = item.attrtype;
                    attrname[i] = p[5+4*i];
                    //重命名

                    break;
                }
            }
        }


        int sattrid = 0;
        String sattrtype = null;
        for (ClassTableItem item:classt.classTable) {
            if (item.classid == classid && item.attrname.equals(p[3+4*attrnumber])) {
                sattrid = item.attrid;
                sattrtype = item.attrtype;
                break;
            }
        }


        for(ObjectTableItem item : topt.objectTable){
            if(item.classid == classid){
                Tuple tuple = GetTuple(item.blockid,item.offset);
                if(Condition(sattrtype,tuple,sattrid,p[4*attrnumber+5])){
                    //Switch

                    for(int j = 0;j<attrnumber;j++){
                        if(Integer.parseInt(p[3+4*j])==1){
                            int value = Integer.parseInt(p[4+4*j]);
                            int orivalue = Integer.parseInt((String)tuple.tuple[attrid[j]]);
                            Object ob = value+orivalue;
                            tuple.tuple[attrid[j]] = ob;
                        }

                    }



                    tpl.addTuple(tuple);
                }
            }
        }
        for(int i =0;i<attrnumber;i++){
            attrid[i]=i;
        }
        if (show) PrintSelectResult(tpl,attrname,attrid,attrtype);
        return tpl;

    }


    // 10,2,un,name,0,0,n,salary,1,10,s,company,salary,=,1000,name,0,0,n,salary,0,0,s,company,name,=,"cc"
    // 0  1 2  3    4 5 6 7      8 9  1011      12     1314   15   16171819     20212223      24   2526
    private void CreateUnionDeputy(String[] p) {

        // get the Tuple result by UnionOp() but it's USELESS, only for reference
        //ArrayList<String> list = new ArrayList<String>();
        //list.add("9");list.add("6");
        //list.add(p[1]);list.add("UNION");list.add("6");list.add(p[1]);
        // for (int i = 3; i < p.length; i++) {
        //    list.add(p[i]);
        // }
        // String[] p_union = new String[list.size()];
        // list.toArray(p_union);
        // TupleList right_tpl = UnionOp(p_union, true);

        int count = Integer.parseInt(p[1]);
        String classname = p[2];//代理类的名字
        String bedeputyname1 = p[4*count+3];//代理的类1的名字
        String bedeputyname2 = p[8*count+7];//代理的类2的名字
        classt.maxid++;
        int classid = classt.maxid;//代理类的id
        int bedeputyid1 = -1, bedeputyid2 = -1;//代理的类的id
        String[] attrname=new String[count];
        String[] bedeputyattrname1 = new String[count];
        String[] bedeputyattrname2 = new String[count];
        int[] bedeputyattrid1 = new int[count];
        int[] bedeputyattrid2 = new int[count];
        String[] attrtype = new String[count];
        int[] attrid = new int[count];

        for(int j = 0;j<count;j++) {
            attrname[j] = p[4*j+6];
            attrid[j] = j;
            bedeputyattrname1[j] = p[4*j+3];
            bedeputyattrname2[j] = p[4*j+4*count+7];
        }

        // ClassTable, SwitchingTable
        ArrayList<ClassTableItem> cList = new ArrayList<ClassTableItem>();
        for (int i = 0; i < count; i++) {
            boolean class1_found = false, class2_found = false;
            // 找到每个被代理属性的id
            for (ClassTableItem item:classt.classTable) {
                if (item.classname.equals(bedeputyname1)&&item.attrname.equals(p[3 + 4 * i])) { // 被代理属性名1
                    bedeputyid1 = item.classid;
                    bedeputyattrid1[i] = item.attrid;
                    // 新增类表项
                    cList.add(new ClassTableItem(classname, classid, count,attrid[i],attrname[i], item.attrtype,"de"));
                    attrtype[i] = item.attrtype;
                    // 新增转换表表项
                    if(Integer.parseInt(p[4+4*i])==1){
                            switchingT.switchingTable.add(new SwitchingTableItem(item.attrname,attrname[i],p[5 + 4 * i]));
                    }
                    if(Integer.parseInt(p[4+4*i])==0) {
                            switchingT.switchingTable.add(new SwitchingTableItem(item.attrname, attrname[i], "0"));
                    }
                    class1_found = true;
                }
                if (item.classname.equals(bedeputyname2)&&item.attrname.equals(p[4*i+4*count+7])) { // 被代理属性名2
                    bedeputyid2 = item.classid;
                    bedeputyattrid2[i] = item.attrid;
                    // 新增转换表表项
                    if (Integer.parseInt(p[4*i+4*count+8]) == 1) {
                        if (!(p[4*i+4*count+9].equals(p[5+4*i]) && bedeputyname1.equals(bedeputyname2)))
                            switchingT.switchingTable.add(new SwitchingTableItem(item.attrname, attrname[i], p[4*i+4*count+9]));
                    }
                    if (Integer.parseInt(p[4*i+4*count+8]) == 0) {
                        if (!(p[4*i+4*count+9].equals(p[5+4*i]) && bedeputyname1.equals(bedeputyname2)))
                            switchingT.switchingTable.add(new SwitchingTableItem(item.attrname, attrname[i], "0"));
                    }
                    class2_found = true;
                }
                if (class1_found && class2_found) break;
            }
        }

        for (ClassTableItem item1: cList) {
            classt.classTable.add(item1);
        }

        // DeputyTable
        // 提取代理条件，新增代理表项
        String[] con1 =new String[3];
        con1[0] = p[4+4*count];
        con1[1] = p[5+4*count];
        con1[2] = p[6+4*count];
        deputyt.deputyTable.add(new DeputyTableItem(bedeputyid1,classid,con1));

        String[] con2 =new String[3];
        con2[0] = p[8+8*count];
        con2[1] = p[9+8*count];
        con2[2] = p[10+8*count];
        deputyt.deputyTable.add(new DeputyTableItem(bedeputyid2,classid,con2));

        // 寻找条件1左边属性的id
        int conid = 0;
        String contype = null;
        for(ClassTableItem item3:classt.classTable){
            if(item3.attrname.equals(con1[0])){
                conid = item3.attrid;
                contype = item3.attrtype;
                break;
            }
        }

        // ObjectTable, BiPointerTable
        TupleList tpl = new TupleList();
        List<ObjectTableItem> obj = new ArrayList<>();
        for(ObjectTableItem item2:topt.objectTable){ // 遍历全部object表项
            if(item2.classid == bedeputyid1){ // 筛选出被代理类1的表项
                Tuple tuple = GetTuple(item2.blockid,item2.offset);
                if (Condition(contype,tuple,conid,con1[2])) { // 如果满足select条件
                    // 新建元组
                    Tuple ituple = new Tuple();
                    ituple.tupleHeader = count;
                    ituple.tuple = new Object[count];

                    for(int o =0;o<count;o++){  // 逐个属性复制
                        if(Integer.parseInt(p[4+4*o]) == 1){ // 属性代理规则：有偏移量
                            int value = Integer.parseInt(p[5+4*o]);
                            int orivalue = Integer.parseInt((String)tuple.tuple[bedeputyattrid1[o]]);
                            Object ob = value + orivalue;
                            ituple.tuple[o] = ob;
                        }
                        if(Integer.parseInt(p[4+4*o]) == 0){
                            ituple.tuple[o] = tuple.tuple[bedeputyattrid1[o]];
                        }
                    }
                    tpl.addTuple(ituple);

                    // 新表项加入obj
                    topt.maxTupleId++;
                    int tupid = topt.maxTupleId;

                    // 加入存储结构
                    int [] aa = InsertTuple(ituple);
                    obj.add(new ObjectTableItem(classid,tupid,aa[0],aa[1]));

                    // BiPointerTable
                    biPointerT.biPointerTable.add(new BiPointerTableItem(bedeputyid1,item2.tupleid,classid,tupid));

                }
            }
        }

        // 寻找条件2左边属性的id
        for(ClassTableItem item3:classt.classTable){
            if(item3.attrname.equals(con2[0])){
                conid = item3.attrid;
                contype = item3.attrtype;
                break;
            }
        }

        for(ObjectTableItem item2:topt.objectTable){ // 遍历全部object表项
            if(item2.classid == bedeputyid2){ // 筛选出被代理类2的表项
                Tuple tuple = GetTuple(item2.blockid,item2.offset);
                if(Condition(contype,tuple,conid,con2[2])) { // 如果满足select条件
                    // 新建元组
                    Tuple ituple = new Tuple();
                    ituple.tupleHeader = count;
                    ituple.tuple = new Object[count];

                    for(int o =0;o<count;o++){  // 逐个属性复制
                        if(Integer.parseInt(p[4*o+4*count+8]) == 1){ // 属性代理规则：有偏移量
                            int value = Integer.parseInt(p[4*o+4*count+9]);
                            int orivalue = Integer.parseInt((String)tuple.tuple[bedeputyattrid2[o]]);
                            Object ob = value+orivalue;
                            ituple.tuple[o] = ob;
                        }
                        if(Integer.parseInt(p[4*o+4*count+8]) == 0){
                            ituple.tuple[o] = tuple.tuple[bedeputyattrid2[o]];
                        }
                    }

                    // 判断元组是否重复
                    boolean addable = true;
                    for (int i = 0; i < tpl.tuplenum; i++) {
                        if (tpl.compareTuples(ituple, tpl.tuplelist.get(i))) {
                            addable = false;
                            break;
                        }
                    }
                    if (addable) {
                        tpl.addTuple(ituple);
                        // 新表项加入obj
                        topt.maxTupleId++;
                        int tupid = topt.maxTupleId;
                        // 加入存储结构
                        int[] aa = InsertTuple(ituple);
                        obj.add(new ObjectTableItem(classid, tupid, aa[0], aa[1]));
                        //更新双向指针表
                        biPointerT.biPointerTable.add(new BiPointerTableItem(
                                bedeputyid2, item2.tupleid, classid, tupid
                        ));
                    }
                }
            }
        }
        // obj全部插入ObjectTable
        for(ObjectTableItem item6:obj) {
            topt.objectTable.add(item6);
        }

        PrintSelectResult(tpl,attrname,attrid,attrtype);
    }

    //CREATE SELECTDEPUTY aa SELECT  b1+2 AS c1,b2 AS c2,b3 AS c3 FROM  bb WHERE t1="1" ;
    //2,3,aa,b1,1,2,c1,b2,0,0,c2,b3,0,0,c3,bb,t1,=,"1"
    //0 1 2  3  4 5 6  7  8 9 10 11 121314 15 16 17 18
    private void CreateSelectDeputy(String[] p) {

        int count = Integer.parseInt(p[1]);
        String classname = p[2];//代理类的名字
        String bedeputyname = p[4*count+3];//代理的类的名字
        classt.maxid++;
        int classid = classt.maxid;//代理类的id
        int bedeputyid = -1;//代理的类的id
        String[] attrname=new String[count];
        String[] bedeputyattrname=new String[count];
        int[] bedeputyattrid = new int[count];
        String[] attrtype=new String[count];
        int[] attrid=new int[count];
        for(int j = 0;j<count;j++){
            attrname[j] = p[4*j+6];
            attrid[j] = j;  //代理类中属性的id从0开始
            bedeputyattrname[j] = p[4*j+3];
        }

        String attrtype1;
        for (int i = 0; i < count; i++) {

            // 找到每个被代理属性的id
            for (ClassTableItem item:classt.classTable) {
                if (item.classname.equals(bedeputyname)&&item.attrname.equals(p[3+4*i])) { // 被代理属性名
                    bedeputyid = item.classid;
                    bedeputyattrid[i] = item.attrid;
                        // 新增类表项
                        classt.classTable.add(new ClassTableItem(classname, classid, count,attrid[i],attrname[i], item.attrtype,"de"));
                        // 新增转换表表项
                        if(Integer.parseInt(p[4+4*i]) == 1){
                            switchingT.switchingTable.add(new SwitchingTableItem(item.attrname,attrname[i],p[5+4*i]));
                        }
                        if(Integer.parseInt(p[4+4*i])==0){
                            switchingT.switchingTable.add(new SwitchingTableItem(item.attrname,attrname[i],"0"));
                        }
                    break;
                }
            };
        }

        // 提取代理条件，新增代理表项
        String[] con =new String[3];
        con[0] = p[4+4*count];
        con[1] = p[5+4*count];
        con[2] = p[6+4*count];
        deputyt.deputyTable.add(new DeputyTableItem(bedeputyid,classid,con));


        TupleList tpl= new TupleList();

        // 寻找条件左边属性的id
        int conid = 0;
        String contype  = null;
        for(ClassTableItem item3:classt.classTable){
            if(item3.attrname.equals(con[0])){
                conid = item3.attrid;
                contype = item3.attrtype;
                break;
            }
        }
        List<ObjectTableItem> obj = new ArrayList<>();
        for(ObjectTableItem item2:topt.objectTable){ // 遍历全部object表项
            if(item2.classid == bedeputyid){ // 筛选出被代理类的表项
                Tuple tuple = GetTuple(item2.blockid,item2.offset);
                if(Condition(contype,tuple,conid,con[2])) { // 如果满足select条件
                    // 新建元组
                    Tuple ituple = new Tuple();
                    ituple.tupleHeader = count;
                    ituple.tuple = new Object[count];

                    for(int o =0;o<count;o++){  // 逐个属性复制
                        if(Integer.parseInt(p[4+4*o]) == 1){ // 属性代理规则：有偏移量
                            int value = Integer.parseInt(p[5+4*o]);
                            int orivalue =Integer.parseInt((String)tuple.tuple[bedeputyattrid[o]]);
                            Object ob = value+orivalue;
                            ituple.tuple[o] = ob;
                        }
                        if(Integer.parseInt(p[4+4*o]) == 0){
                            ituple.tuple[o] = tuple.tuple[bedeputyattrid[o]];
                        }
                    }

                    // 新表项加入obj
                    topt.maxTupleId++;
                    int tupid = topt.maxTupleId;

                    // 加入存储结构
                    int [] aa = InsertTuple(ituple);
                    obj.add(new ObjectTableItem(classid,tupid,aa[0],aa[1]));

                    //更新双向指针表
                    biPointerT.biPointerTable.add(new BiPointerTableItem(bedeputyid,item2.tupleid,classid,tupid));

                }
            }
        }
        // obj全部插入ObjectTable
        for(ObjectTableItem item6:obj) {
            topt.objectTable.add(item6);
        }
    }

    //SELECT popSinger -> singer.nation  FROM popSinger WHERE singerName = "JayZhou";
    //7,2,popSinger,singer,nation,popSinger,singerName,=,"JayZhou"
    //0 1 2         3      4      5         6          7  8
    private TupleList InDirectSelect(String[] p, boolean show){
        TupleList tpl= new TupleList();
        String classname = p[3];
        String attrname = p[4];
        String crossname = p[2];
        String[] attrtype = new String[1];
        String[] con =new String[3];
        con[0] = p[6];
        con[1] = p[7];
        con[2] = p[8];

        int classid = 0;
        int crossid = 0;
        String crossattrtype = null;
        int crossattrid = 0;
        for(ClassTableItem item : classt.classTable){
            if(item.classname.equals(classname)){
                classid = item.classid;
                if(attrname.equals(item.attrname))
                    attrtype[0]=item.attrtype;
            }
            if(item.classname.equals(crossname)){
                crossid = item.classid;
                if(item.attrname.equals(con[0])) {
                    crossattrtype = item.attrtype;
                    crossattrid = item.attrid;
                }
            }
        }

        for(ObjectTableItem item1:topt.objectTable){
            if(item1.classid == crossid){
                Tuple tuple = GetTuple(item1.blockid,item1.offset);
                if(Condition(crossattrtype,tuple,crossattrid,con[2])){
                    for(BiPointerTableItem item3: biPointerT.biPointerTable){
                        if(item1.tupleid == item3.objectid&&item3.deputyid == classid){
                            for(ObjectTableItem item2: topt.objectTable){
                                if(item2.tupleid == item3.deputyobjectid){
                                    Tuple ituple = GetTuple(item2.blockid,item2.offset);
                                    tpl.addTuple(ituple);
                                }
                            }
                        }
                    }

                }
            }

        }
        String[] name = new String[1];
        name[0] = attrname;
        int[] id = new int[1];
        id[0] = 0;
        if (show) PrintSelectResult(tpl,name,id,attrtype);
        return tpl;




    }

    //UPDATE Song SET type = ‘jazz’WHERE songId = 100;
    //OPT_CREATE_UPDATE，Song，type，“jazz”，songId，=，100
    //0                  1     2      3        4      5  6
    private void Update(String[] p){
        String classname = p[1];
        String attrname = p[2];
        String cattrname = p[4];//condition attribute name

        int classid = 0;
        int attrid = 0;
        String attrtype = null;
        int cattrid = 0;
        String cattrtype = null;
        for(ClassTableItem item :classt.classTable){
            if (item.classname.equals(classname)){
                classid = item.classid;
                break;
            }
        }//确定classid
        for(ClassTableItem item1 :classt.classTable){
            if (item1.classid==classid&&item1.attrname.equals(attrname)){
                attrtype = item1.attrtype;
                attrid = item1.attrid;
            }
            //这里不需要break吗？
        }
        for(ClassTableItem item2 :classt.classTable){
            if (item2.classid==classid&&item2.attrname.equals(cattrname)){
                cattrtype = item2.attrtype;
                cattrid = item2.attrid;
            }//这里和上一个循环干了差不多的事情
        }



        int _tupleid=0;
        int flag=0;
        for(ObjectTableItem item3:topt.objectTable){
            if(item3.classid == classid){
                Tuple tuple = GetTuple(item3.blockid,item3.offset);
                if(Condition(cattrtype,tuple,cattrid,p[6])){
                    flag=1;
                    _tupleid= item3.tupleid;
                    System.out.println("tupleid:    "+_tupleid);
                    //UpdatebyID(item3.tupleid,attrid,p[3].replace("\"",""));
                    break;
                }
            }
        }
        if(flag==1){
            UpdatebyID(_tupleid,attrid,p[3].replace("\"",""),true);
        }


    }

    private void UpdatebyID(int tupleid,int attrid,String value,boolean isSrc){
        for(ObjectTableItem item: topt.objectTable){
            if(item.tupleid ==tupleid){
                Tuple tuple = GetTuple(item.blockid,item.offset);
                tuple.tuple[attrid] = value;
                UpateTuple(tuple,item.blockid,item.offset);
                Tuple tuple1 = GetTuple(item.blockid,item.offset);
                UpateTuple(tuple1,item.blockid,item.offset);
            }
        }
        //System.out.println("stage1  "+tupleid);
        String attrname = null;
        for(ClassTableItem item2: classt.classTable){
            if (item2.attrid == attrid){
                attrname = item2.attrname;
                break;
            }
        }
        //System.out.println("stage2  "+tupleid);
        for(BiPointerTableItem item1: biPointerT.biPointerTable) {
            if (item1.objectid == tupleid) {
                for(ClassTableItem item4:classt.classTable){
                    if(item4.classid==item1.deputyid){
                        String dattrname = item4.attrname;
                        int dattrid = item4.attrid;
                        for (SwitchingTableItem item5 : switchingT.switchingTable) {
                            String dswitchrule = null;
                            String dvalue = null;
                            if (item5.attr.equals(attrname) && item5.deputy.equals(dattrname)) {
                                dvalue = value;
                                if (Integer.parseInt(item5.rule) != 0) {
                                    dswitchrule = item5.rule;
                                    dvalue = Integer.toString(Integer.parseInt(dvalue) + Integer.parseInt(dswitchrule));
                                }
                                //System.out.println("stage3  "+item1.deputyobjectid);
                                UpdatebyID(item1.deputyobjectid, dattrid, dvalue,false);//此处更新代理类
                                break;
                            }
                        }
                    }
                }
            }
        }
        //System.out.println("stage3  "+tupleid);
        //增删操作
        //System.out.println("增删操作开始");
        if(isSrc){
            OandB ob2=new OandB();
            int flag=0;
            ObjectTableItem newobj = null;
            BiPointerTableItem newbip = null;

            for(ObjectTableItem item6 : topt.objectTable){
                int classid = 0;
                //System.out.println("stage4  "+tupleid);
                if(item6.tupleid == tupleid){
                    //找到这个元组的所有代理元组以及对应规则
                    Tuple tuple = GetTuple(item6.blockid,item6.offset);
                    classid = item6.classid;
                    //System.out.println("增删操作tupleid    "+tupleid);
                    //System.out.println("增删操作classid    "+classid);
                    int deputyid=0;//代理类ID
                    String[] rule={};//代理类的规则
                    int deputyobjid=0;//在代理类中的元组

                    //System.out.println("deputytable遍历");
                    for(DeputyTableItem item8: deputyt.deputyTable){
                        if(item8.originid==classid){
                            deputyid = item8.deputyid;
                            rule = item8.deputyrule;
                            break;
                        }
                    }
                    //System.out.println("bipointer遍历");
                    for(BiPointerTableItem item7: biPointerT.biPointerTable){
                        if(item6.tupleid==item7.objectid&&item7.deputyid==deputyid){
                            deputyobjid=item7.deputyobjectid;
                            break;//假设一个元组只会被一个类代理
                        }
                    }

                    //对rule做格式化处理
                    System.out.println("rule如下");
                    if(rule.length!=0){
                        System.out.println(rule[0]);
                        System.out.println(rule[1]);
                        System.out.println(rule[2]);
                    }
                    else{
                        System.out.println("rule为空");
                        System.out.println(classid);
                    }
                    System.out.println(rule);
                    String cattrname = rule[0];
                    int cattrid = 0;
                    String cattrtype = null;
                    for(ClassTableItem item10 :classt.classTable){
                        if (item10.classid==classid&&item10.attrname.equals(cattrname)){
                            cattrtype = item10.attrtype;
                            cattrid = item10.attrid;
                        }
                    }


                    if(deputyobjid!=0){//在deputy
                        if(Condition(cattrtype,tuple,cattrid,rule[2])){//满足rule
                            ;//donothing
                        }
                        else{//不满足rule
                            //删去
                            flag=1;
                            OandB ob =new OandB(DeletebyID(deputyobjid));
                            for(ObjectTableItem obj:ob.o){
                                ob2.o.add(obj);
                            }
                            for(BiPointerTableItem bip:ob.b){
                                ob2.b.add(bip);
                            }
                        /*
                        for(ObjectTableItem obj: ob2.o){
                            topt.objectTable.remove(obj);
                        }
                        for(BiPointerTableItem bip : ob2.b) {
                            biPointerT.biPointerTable.remove(bip);
                        }
                        */

                        }
                    }
                    else{//不在deputy
                        //System.out.println("准备插入代理类");
                        if(Condition(cattrtype,tuple,cattrid,rule[2])){//满足rule
                            //增加
                            //System.out.println("条件判断正确");
                            flag=2;
                            int[] a = InsertTuple(tuple);
                            topt.maxTupleId++;
                            int new_tupleid=topt.maxTupleId;
                            newobj = new ObjectTableItem(deputyid,new_tupleid,a[0],a[1]);
                            //topt.objectTable.add(new ObjectTableItem(classid,new_tupleid,a[0],a[1]));
                            int new_deputyobjid=new_tupleid+1;
                            newbip = new BiPointerTableItem(classid,new_tupleid,deputyid,new_deputyobjid);
                            //biPointerT.biPointerTable.add(new BiPointerTableItem(classid,new_tupleid,deputyid,new_deputyobjid));

                        }
                        else{
                            ;//donothing
                        }
                    }

                }
            }
            if(flag==1){
                for(ObjectTableItem obj: ob2.o){
                    topt.objectTable.remove(obj);
                }
                for(BiPointerTableItem bip : ob2.b) {
                    biPointerT.biPointerTable.remove(bip);
                }
            }
            else if(flag==2){
                topt.objectTable.add(newobj);
                biPointerT.biPointerTable.add(newbip);

            }
        }
    }



        //INSERT INTO aa VALUES (1,2,"3");
        //4,3,aa,1,2,"3"







    private class OandB{
        public List<ObjectTableItem> o= new ArrayList<>();
        public List<BiPointerTableItem> b= new ArrayList<>();
        public OandB(){}
        public OandB(OandB oandB){
            this.o = oandB.o;
            this.b = oandB.b;
        }

        public OandB(List<ObjectTableItem> o, List<BiPointerTableItem> b) {
            this.o = o;
            this.b = b;
        }
    }




    private Tuple GetTuple(int id, int offset) {

        return mem.readTuple(id,offset);
    }

    private int[] InsertTuple(Tuple tuple){
        return mem.writeTuple(tuple);
    }

    private void DeleteTuple(int id, int offset){
        mem.deleteTuple();
        return;
    }

    private void UpateTuple(Tuple tuple,int blockid,int offset){
        mem.UpateTuple(tuple,blockid,offset);
    }

    private void PrintTab(ObjectTable topt,SwitchingTable switchingT,DeputyTable deputyt,BiPointerTable biPointerT,ClassTable classTable) {
        Intent intent = new Intent(context, ShowTable.class);

        Bundle bundle0 = new Bundle();
        bundle0.putSerializable("ObjectTable",topt);
        bundle0.putSerializable("SwitchingTable",switchingT);
        bundle0.putSerializable("DeputyTable",deputyt);
        bundle0.putSerializable("BiPointerTable",biPointerT);
        bundle0.putSerializable("ClassTable",classTable);
        intent.putExtras(bundle0);
        context.startActivity(intent);


    }

    private void PrintSelectResult(TupleList tpl, String[] attrname, int[] attrid, String[] type) {
        Intent intent = new Intent(context, PrintResult.class);

        for (Tuple t: tpl.tuplelist) {
            System.out.println(t.tuple[0]);
        }
        Bundle bundle = new Bundle();
        bundle.putSerializable("tupleList", tpl);
        bundle.putStringArray("attrname", attrname);
        bundle.putIntArray("attrid", attrid);
        bundle.putStringArray("type", type);
        intent.putExtras(bundle);
        context.startActivity(intent);


    }
    public void PrintTab(){
        PrintTab(topt,switchingT,deputyt,biPointerT,classt);
    }

    public String baidu = "[[-8.639847,41.159826],[-8.640351,41.159871],[-8.642196,41.160114],[-8.644455,41.160492],[-8.646921,41.160951],[-8.649999,41.161491],[-8.653167,41.162031],[-8.656434,41.16258],[-8.660178,41.163192],[-8.663112,41.163687],[-8.666235,41.1642],[-8.669169,41.164704],[-8.670852,41.165136],[-8.670942,41.166576],[-8.66961,41.167962],[-8.668098,41.168988],[-8.66664,41.170005],[-8.665767,41.170635],[-8.66574,41.170671]]";
    public String didi = "[[-8.615907,41.140557],[-8.614449,41.141088],[-8.613522,41.14143],[-8.609904,41.140827],[-8.609301,41.139522],[-8.609544,41.138865],[-8.610777,41.137551],[-8.611452,41.136012],[-8.610624,41.134563],[-8.609319,41.134446],[-8.608014,41.1345],[-8.607987,41.134536],[-8.607987,41.134518],[-8.607861,41.134536],[-8.60778,41.134545],[-8.607411,41.134527],[-8.605476,41.134392],[-8.604603,41.134176],[-8.604594,41.134158]]";
    public String hello_bike = "[[-8.619894,41.148009],[-8.620164,41.14773],[-8.62065,41.148513],[-8.62092,41.150313],[-8.621208,41.151951],[-8.621118,41.153517],[-8.620884,41.155416],[-8.620938,41.155479],[-8.620974,41.155461],[-8.621028,41.155461],[-8.619777,41.155344],[-8.619282,41.155335],[-8.618112,41.155101],[-8.61534,41.154579],[-8.613297,41.153994],[-8.612064,41.153832],[-8.611911,41.155227],[-8.611794,41.156838],[-8.610804,41.157171],[-8.61021,41.15727],[-8.609508,41.157333],[-8.60949,41.157351]]";
    public String nike_run = "[[-8.618868,41.155101],[-8.6175,41.154912],[-8.615079,41.154525],[-8.613468,41.154228],[-8.613261,41.154102],[-8.613297,41.153832],[-8.612037,41.153904],[-8.611929,41.155803],[-8.610876,41.157171],[-8.610183,41.157252],[-8.610138,41.15727],[-8.609508,41.157369],[-8.608707,41.158395],[-8.607915,41.160042],[-8.607654,41.1606],[-8.606295,41.164155],[-8.60643,41.166693],[-8.60634,41.169123],[-8.60445,41.171112],[-8.60121,41.171166],[-8.597205,41.171625],[-8.593578,41.170968],[-8.5905,41.168979],[-8.587206,41.167062],[-8.583624,41.166252],[-8.58213,41.1642],[-8.58114,41.163381],[-8.579376,41.164326],[-8.577468,41.165316],[-8.576298,41.163858],[-8.575101,41.16231],[-8.575065,41.162265]]";

    private ArrayList<ArrayList<Float>> stringToList(String s) {
        ArrayList<ArrayList<Float>> ret = new ArrayList<ArrayList<Float>>();
        int index1 = s.indexOf("["), index2 = s.lastIndexOf("]");
        s = s.substring(index1 + 1, index2);
        s = s.trim();
        String[] pairs_split = s.split(",");
        ArrayList<String> pairs = new ArrayList<String>();
        for (int i = 0; i < pairs_split.length; i += 2) {
            pairs.add(pairs_split[i] + "," + pairs_split[i + 1]);
        }
        for (String sitem: pairs) {
            sitem = sitem.trim();
            index1 = sitem.indexOf("[");
            index2 = sitem.lastIndexOf("]");
            sitem = sitem.substring(index1 + 1, index2);
            String[] nums = sitem.split(",");
            nums[0] = nums[0].trim();
            nums[1] = nums[1].trim();
            ArrayList<Float> tmp = new ArrayList<Float>();
            tmp.add(Float.parseFloat(nums[0]));
            tmp.add(Float.parseFloat(nums[1]));
            ret.add(tmp);
        }
        return ret;
    }

    public void TrackCreate() {
        ArrayList<ArrayList<Float>> track_baidu = stringToList(baidu);
        ArrayList<ArrayList<Float>> track_didi = stringToList(didi);
        ArrayList<ArrayList<Float>> track_hello = stringToList(hello_bike);
        ArrayList<ArrayList<Float>> track_nike = stringToList(nike_run);
        String q = "CREATE CLASS baidu(x1 float, y1 float, ee1 int);";
        query(q, false);
        for (ArrayList<Float> pos: track_baidu) {
            q = "INSERT INTO baidu VALUES (" + pos.get(0).toString() + "," + pos.get(1).toString() + ",1);";
            query(q, false);
        }
        q = "CREATE CLASS didi(x2 float, y2 float, ee2 int);";
        query(q, false);
        for (ArrayList<Float> pos: track_didi) {
            q = "INSERT INTO didi VALUES (" + pos.get(0).toString() + "," + pos.get(1).toString() + ",1);";
            query(q, false);
        }
        q = "CREATE CLASS hello(x3 float, y3 float, ee3 int);";
        query(q, false);
        for (ArrayList<Float> pos: track_hello) {
            q = "INSERT INTO hello VALUES (" + pos.get(0).toString() + "," + pos.get(1).toString() + ",1);";
            query(q, false);
        }
        q = "CREATE CLASS nike(x4 float, y4 float, ee4 int);";
        query(q, false);
        for (ArrayList<Float> pos: track_nike) {
            q = "INSERT INTO nike VALUES (" + pos.get(0).toString() + "," + pos.get(1).toString() + ",1);";
            query(q, false);
        }
        new AlertDialog.Builder(context).setTitle("提示").setMessage("轨迹数据导入成功").setPositiveButton("确定",null).show();
    }
    public void TrackMerge() {
        String q = "CREATE UNIONDEPUTY un1 SELECT x1 AS x,y1 AS y,ee1 AS ee FROM baidu WHERE ee1=1 UNION SELECT x2 AS x,y2 AS y,ee2 AS ee FROM didi WHERE ee2=1;";
        query(q, false);
        q = "CREATE UNIONDEPUTY un2 SELECT x AS x,y AS y,ee AS ee FROM un1 WHERE ee=1 UNION SELECT x3 AS x,y3 AS y,ee3 AS ee FROM hello WHERE ee3=1;";
        query(q, false);
        q = "CREATE UNIONDEPUTY un3 SELECT x AS x,y AS y,ee AS ee FROM un2 WHERE ee=1 UNION SELECT x4 AS x,y4 AS y,ee4 AS ee FROM nike WHERE ee4=1;";
        query(q, false);
    }
}