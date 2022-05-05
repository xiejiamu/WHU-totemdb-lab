package drz.oddb.Memory;
import java.io.Serializable;
import java.util.*;
public class TupleList implements Serializable {
    public List<Tuple> tuplelist = new ArrayList<Tuple>();
    public int tuplenum = 0;

    public void addTuple(Tuple tuple){
        this.tuplelist.add(tuple);
        tuplenum++;
    }

    // Edited by Xukeyizhi
    // join another tuplelist to it.
    public void joinTuple(TupleList list){
        this.tuplelist.addAll(list.tuplelist);
        tuplenum += list.tuplenum;
    }

    // Edited by Xukeyizhi
    // union another tuplelist to it.
    public void joinTupleUnion(TupleList list){
        int original_num = tuplenum;
        for (int i = 0; i < list.tuplenum; i++) {
            boolean addable = true;
            for (int j = 0; j < original_num; j++) {
                System.out.println("Indexes of Tuples to compare:ori[" + j + "],new[" + i+"]");
                if (compareTuples(list.tuplelist.get(i), tuplelist.get(j))) {
                    addable = false;
                    break;
                }
            }
            if (addable) addTuple(list.tuplelist.get(i));
        }
    }

    // Edited by Xukeyizhi
    // compare two tuples, if equal return true
    public boolean compareTuples(Tuple t1, Tuple t2) {
        if (t1.tupleHeader != t2.tupleHeader) {
            return false;
        } else {
            boolean retval = true;

            for (int i = 0; i < t1.tupleHeader; i++) {
                Object param1 = t1.tuple[i];
                Object param2 = t2.tuple[i];
                if (param1 instanceof Integer) {
                    Integer v1 = ((Integer)param1).intValue();
                    Integer v2 = ((Integer)param2).intValue();
                    if (v1 != v2) {
                        retval = false;
                        break;
                    }
                } else if (param1 instanceof String) {
                    String s1 = (String)param1;
                    String s2 = (String)param2;
                    if (!s1.equals(s2)) {
                        retval = false;
                        break;
                    }
                } else if (param1 instanceof Float) {
                    Float f1 = ((Float)param1).floatValue();
                    Float f2 = ((Float)param2).floatValue();
                    if (!f1.equals(f2)) {
                        retval = false;
                        break;
                    }
                } else {
                    retval = false;
                }
            }
            return retval;
        }
    }

}
