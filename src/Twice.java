import java.util.*;

public class Twice {
    public static final void main(String[] args) {
    	ArrayList<Integer> al = new ArrayList<Integer>();
        Scanner s = new Scanner(System.in);
        while(s.hasNext()) {
            int i = new Integer(s.next());
            System.out.println(i);
            al.add( i );
        }
        for ( int i = 0; i < al.size(); i++ ) {
        	int j = al.get(i);
        	System.out.println( j );
        }
    }
}
