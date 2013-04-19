public class DataWrite
{
public static void main(String[] args)
{
	int inst = 0x03e00008; // jr $31
	System.out.write( (inst >> 24) & 0xFF );
	System.out.write( (inst >> 16) & 0xFF );
	System.out.write( (inst >> 8) & 0xFF );
	System.out.write( inst & 0xFF );
	System.out.flush(); // Remember to force the stream to
	// be outputted ("flushing")
	}
}