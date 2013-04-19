import java.util.*;
import java.math.*;
import java.io.*;

/** A sample main class demonstrating the use of the Lexer.
 *  This main class just outputs each line in the input, followed by
 *  the tokens returned by the lexer for that line.
 *
 *  If a filename (or path) is supplied as the first argument, eg
 *       java Asm   src/sumOneToFive.asm
 *  this program will read from that file; if no argument is present,
 *  this program will read from standard input, whether typed directly
 *  on the keyboard or redirected at the command line from a file, as by
 *       java Asm < src/sumOneToFive.asm
 *
 *  Requires Java version 1.5
 *
 *  Minor modifications by JCBeatty, Jan 2009.
 *
 *  A very quick summary of Java language features used in this program that
 *  few graduates of CS 134 will have encountered follow.
 *
 *  (1) The definition of multiple classes in a single file. This is generally a bad idea:
 *      (a) many java tools, such as Sun's java compiler javac, locate the source for a
 *      class by looking for a file whose name matches the class name; (b) it is typically
 *      more difficulty to work with a program, especially a large program, that's defined
 *      in a single file. It is convenient for CS 241, however, because electronic submission
 *      of a program requires submitting only a single file.
 *
 *  (2) "enums" - that is, "enumerated types". In CS 134, you learned to create symbolic constants
 *      by using statements such as "static final int meaningOfTheUniverse = 42". Roughly speaking,
 *      enums are a way of asking the java compiler to create a set of such symbolic constants
 *      having distinct values. The enumerated type Kind defined in this file is a good example.
 *      However, enums are actually a special kind of class and can have constructors and methods.
 *      The enumerated type State defined much later in this file illustrates this.
 *
 *  (3) "Parametric types." This is a huge topic; Java's implementation has many warts and
 *      confusing corner cases. However, their only appearance here is in the declaration of an
 *      ArrayList of Tokens in the scan method below. [The Java class ArrayList (actually
 *      java.util.ArrayList) is Sun's version of the CS 134 ListArray class; the "List" you
 *      see here is also imported from java.util, and is analogous to (but not the same as)
 *      the List interface defined in CS 134.] So 
 *          List<Token> ret = new ArrayList<Token>();
 *      allows the compiler to take care of ensuring that you only put Tokens into ret,
 *      and to automatically cast objects you retrieve from ret into Tokens so that you don't
 *      have to.
 *
 *  (4) Nested classes and interfaces. There are examples of both below, in the definition of the
 *      class Lexer: State, Chars, AllChars and Transition are nexted class definitions; and Chars
 *      is a nested interface definition. Defining these *inside* the definition of Lexer means that
 *      they can only be used by code within Lexer, which is arguably good design if they're not
 *      *intended* to be used elsewhere.
 *
 *  (5) System.exit(0) and System.exit(1). Not surprisingly, calls to System.exit(...) cause the
 *      program to cease execution. When you run a program from a command line shell, the integer
 *      value returned by such an exit is made available to the shell. By convention, unix programs
 *      that quit normally return 0; programs that quit because they have encountered a fatal error
 *      return a non-zero value - often a non-zero error code specifying more-or-less precisely :-)
 *      exactly what error was encountered.
 *
 *  Regarding note (1): if you decide you'd REALLY prefer that each class live in its own file,
 *  it's straightforward to write a simple shell script to merge those files into a single file
 *  for submission, although care must be taken to eliminate multiple identical import statements,
 *  and Asm may be the only public class (as is already true in this file.) For an example of how
 *  to do this, see
 *                   http://jcbServer.cs.uwaterloo.ca/cs241/code/a03/merge.bash
**/
public class Asm {

	private Map<String,Integer> symbolTable = new HashMap<String,Integer>();
	
    // Execution starts here when the program is run from the command line by typing one of...
    //     java Asm < something.asm > something.mips
    //     java Asm   something.asm > something.mips
    public static final void main( String[] args ) {
        // Args contains the sequence of blank-delimited tokens supplied after the name of the class
        // containing main when a java program is executed from the command line.
        if( args.length == 0 )
            new Asm().run( System.in );                               // System.in is an InputStream
        else {
            Asm.exe( args[0] );
        }
    }

    // Called either from main(...) or from JUnit test_...(...) methods in TestCase subclasses.
    public static String exe( String inputFilePath ) {
        try {
            FileInputStream inStream = new FileInputStream( inputFilePath );
            return new Asm().run( inStream );
        } catch( FileNotFoundException e ) {
            throw new Error( "Could not open file \"" + inputFilePath + "\" for reading." );
        }
    }

    // outputs to screens
    private static void toScreen ( Integer number ) {
		System.out.write( (number >> 24) & 0xFF );
		System.out.write( (number >> 16) & 0xFF );
		System.out.write( (number >> 8) & 0xFF );
		System.out.write( number & 0xFF );
    }
    
    // method to handle .word instructions
    private void dotWord ( Token[] tokens, int currentToken ){
    	if ( tokens.length - currentToken != 2 ) {
    		System.err.println( "ERROR, incorrect .word syntax ");
    		System.exit(0);
    	}
    	if ( tokens[ currentToken + 1].kind == Kind.INT || tokens[currentToken + 1].kind == Kind.HEXINT ) {
    	} else if ( tokens[ currentToken + 1].kind == Kind.ID ){
    		
    	}else {
    		System.err.println( "ERROR, not valid int in .word" );
    		System.exit(0);
    	}
    }
      
    // method to handle labels
    private void makeLabel ( String label, int locCounter ){
    	if ( symbolTable.containsKey( label ) ) {
    		System.err.println( " ERROR, duplicate label " + label );
    		System.exit(0);
    	} 
    	symbolTable.put( label , locCounter );
    }
    
    // method to handle jr and jalr instructions
    private void jump ( Token[] tokens, int currentToken ){
    	if ( tokens.length - currentToken != 2 ) {
    		System.err.println( "ERROR, incorrect jump register syntax ");
    		System.exit(0);
    	}
    	if ( tokens[ currentToken + 1].kind == Kind.REGISTER ) {
    		//store register number without the $ sign
    		String regNum = tokens[ currentToken + 1].lexeme.substring(1);
    		checkRegNum( regNum );
    	} else {
    		System.err.println( "ERROR, NOT VALID JR syntax" );
    		System.exit(0);
    	}
    }
      
    // method to handle mfhi, mflo, lis instructions
    private void moves ( Token[] tokens, int currentToken ){
    	if ( tokens.length - currentToken != 2 ) {
    		System.err.println( "ERROR, incorrect jump register syntax ");
    		System.exit(0);
    	}
    	if ( tokens[ currentToken + 1].kind == Kind.REGISTER ) {
    		//store register number without the $ sign
    		String regNum = tokens[ currentToken + 1].lexeme.substring(1);
    		checkRegNum( regNum );
    	} else {
    		System.err.println( "ERROR, NOT VALID moves syntax" );
    		System.exit(0);
    	}
    }
    //check if register is between 0 and 31
    private void checkRegNum ( String regNum ) {
		if ( Integer.valueOf( regNum ) >= 0 &&
				Integer.valueOf( regNum ) < 32 ){
		} else {
			System.err.println( "ERROR, NOT VALID REGISTER" );
    		System.exit(0);
		}
    }
    private void compare (Kind k, Kind target){
    	
    	if (k != target){
    		System.err.println ("ERROR, incorrect format");
    		System.exit(0);
    	}
    	
    }
    // method to handle add, sub, slt, sltu
    private void simpleR ( Token[] tokens, int currentToken ){
    	String temp;
    	if ( tokens.length - currentToken != 6 ) {
    		System.err.println( "ERROR, incorrect simple Register Instruction Length ");
    		System.exit(0);
    	}
    	
    	if(	tokens[ currentToken + 2].kind == Kind.COMMA &&
    			tokens[ currentToken + 4].kind == Kind.COMMA ) {
    		//store register number without the $ sign

    		for ( int i = 1; i <= 5; i+=2 ){
    			compare (tokens[currentToken + i].kind, Kind.REGISTER);
    			temp = tokens[ currentToken + i].lexeme.substring(1);
    			checkRegNum ( temp );
    		}

    	} else {
    		System.err.println( "ERROR, NOT VALID simple Register Instruction syntax" );
    		System.exit(0);
    	}
    }
    
    // method to handle add, sub, slt, sltu
    private void mulDiv ( Token[] tokens, int currentToken ){
    	String temp;
    	if ( tokens.length - currentToken != 4 ) {
    		System.err.println( "ERROR, incorrect mult or div Instruction Length ");
    		System.exit(0);
    	}
    	
    	if(	tokens[ currentToken + 2].kind == Kind.COMMA ) {
    		//store register number without the $ sign

    		for ( int i = 1; i <= 3; i+=2 ){
    			compare (tokens[currentToken + i].kind, Kind.REGISTER);
    			temp = tokens[ currentToken + i].lexeme.substring(1);
    			checkRegNum ( temp );
    		}

    	} else {
    		System.err.println( "ERROR, NOT VALID mult or div Instruction syntax" );
    		System.exit(0);
    	}
    }
    
    // method to handle sw and lw
    private void slWords ( Token[] tokens, int currentToken ){
    	String temp;
    	if ( tokens.length - currentToken != 7 ) {
    		System.err.println( "ERROR, incorrect lw or sw Instruction Length ");
    		System.exit(0);
    	}
    	
    	if(	tokens[ currentToken + 2].kind == Kind.COMMA &&
    			tokens[ currentToken + 4].kind == Kind.LPAREN &&
    			tokens[ currentToken + 6].kind == Kind.RPAREN ) {
    		//store register number without the $ sign

    		for ( int i = 1; i <= 5; i+=4 ){
    			compare (tokens[currentToken + i].kind, Kind.REGISTER);
    			temp = tokens[ currentToken + i].lexeme.substring(1);
    			checkRegNum ( temp );
    		}
    		
    		// check i
    		if ( tokens[ currentToken + 3].kind == Kind.INT ) {
    			if ( tokens[ currentToken + 3].toInt() >= -32768 &&
    					tokens[ currentToken + 3].toInt() <= 32767) {
    				
    			} else {
    				System.err.println( "ERROR, Integer out of range");
    				System.exit(0);
    			}
    		} else if (	tokens[ currentToken + 3].kind == Kind.HEXINT ) {
    			if ( tokens[ currentToken + 3 ].toInt() <= 0xffff ){
    				
    			} else {
    				System.err.println( "ERROR, hex value out of range" );
    				System.exit(0);
    			}
    		} else {
    			System.err.println( "ERROR, invalid lw or sw offset");
    			System.exit(0);
    		}
    		
    	} else {
    		System.err.println( "ERROR, NOT VALID lw or sw Instruction syntax" );
    		System.exit(0);
    	}
    }
    // method to handle bne, beq
    private void branches ( Token[] tokens, int currentToken ){
    	String temp;
    	if ( tokens.length - currentToken != 6 ) {
    		System.err.println( "ERROR, incorrect branch instructions Length ");
    		System.exit(0);
    	}
    	
    	if(	tokens[ currentToken + 2].kind == Kind.COMMA &&
    			tokens[ currentToken + 4].kind == Kind.COMMA ) {
    		//store register number without the $ sign

    		for ( int i = 1; i <= 3; i+=2 ){
    			compare (tokens[currentToken + i].kind, Kind.REGISTER);
    			temp = tokens[ currentToken + i].lexeme.substring(1);
    			checkRegNum ( temp );
    		}
    		
    		// check last element of branch
    		if ( tokens[ currentToken + 5].kind == Kind.INT ) {
    			if ( tokens[ currentToken + 5].toInt() >= -32768 &&
    					tokens[ currentToken + 5].toInt() <= 32767) {
    				
    			} else {
    				System.err.println( "ERROR, Integer out of range");
    				System.exit(0);
    			}
    			
    		} else if (	tokens[ currentToken + 5].kind == Kind.HEXINT ) {
    			if ( tokens[ currentToken + 5 ].toInt() <= 0xffff ){
    				
    			} else {
    				System.err.println( "ERROR, hex value out of range" );
    				System.exit(0);
    			}
    		} else if ( tokens[ currentToken + 5].kind == Kind.ID ) {
    			
    		} else {
    			System.err.println( "ERROR, NOT VALID branch instruction syntax" );
        		System.exit(0);
    		}

    	} else {
    		System.err.println( "ERROR, NOT VALID branch instruction syntax" );
    		System.exit(0);
    	}
    }
    
    //code the JALR instruction
    private void codeJALR( Token[] tokens, int currentToken ){
    	Integer Number = 0x09;
    	Integer S = Integer.valueOf( tokens[currentToken + 1].lexeme.substring(1) );
    	Number = Number | ( S << 21);
    	toScreen( Number );
    }
    
    //code the JR instruction
    private void codeJR( Token[] tokens, int currentToken ){
    	Integer Number = 0x08;
    	Integer S = Integer.valueOf( tokens[currentToken + 1].lexeme.substring(1) );
    	Number = Number | ( S << 21);
    	toScreen( Number );
    }
    
    //code the ADD, SUB, SLT, and ALTU 
    private void codeSimpleR( Token[] tokens, int currentToken, Integer Number ){
    	Integer D = Integer.valueOf( tokens[currentToken + 1].lexeme.substring(1) );
    	Integer S = Integer.valueOf( tokens[currentToken + 3].lexeme.substring(1) );
    	Integer T = Integer.valueOf( tokens[currentToken + 5].lexeme.substring(1) );
    	Number = Number | ( S << 21) | ( T << 16 ) | ( D << 11);
    	toScreen( Number );
    }
    
    //code the LIS, MFHI, MFLO
    private void codeMoves( Token[] tokens, int currentToken, Integer Number ){
    	Integer D = Integer.valueOf( tokens[currentToken + 1].lexeme.substring(1) );
    	Number = Number | ( D << 11);
    	toScreen( Number );
    }
    
    //code the MULT, MULTU, DIV, DIVU
    private void codeMulDiv( Token[] tokens, int currentToken, Integer Number ){
    	Integer S = Integer.valueOf( tokens[currentToken + 1].lexeme.substring(1) );
    	Integer T = Integer.valueOf( tokens[currentToken + 3].lexeme.substring(1) );
    	Number = Number | ( S << 21) | ( T << 16 );
    	toScreen( Number );
    }
    
    //code branches
    private void codeBranches( Token[] tokens, int currentToken, Integer Number, int lineNumber ){
    	Integer OpCode = Number;
    	Integer S = Integer.valueOf( tokens[currentToken + 1].lexeme.substring(1) );
    	Integer T = Integer.valueOf( tokens[currentToken + 3].lexeme.substring(1) );
    	Integer i = 0;
    	int temp = 0;
    	if ( tokens[ currentToken + 5].kind == Kind.ID ){
    		try{
    			temp = symbolTable.get( tokens[ currentToken + 5].lexeme );
    		}catch ( Exception e ){
    			System.err.println("ERROR, The label " + tokens[ currentToken + 5].lexeme + " is not defined" );
				System.exit(0);
    		}
			i = ( temp - lineNumber )/ 4 ;
    	}else{
        	i = ( tokens[currentToken + 5].toInt());
    	}
        	Number = (OpCode << 26 ) | ( S << 21) | ( T << 16 ) | ( i & 0xffff );
    	toScreen( Number );
    }
    
    //code sw and lw 
    private void codeSLWord( Token[] tokens, int currentToken, Integer Number){
    	Integer OpCode = Number;
    	Integer T = Integer.valueOf( tokens[currentToken + 1].lexeme.substring(1) );
    	Integer S = Integer.valueOf( tokens[currentToken + 5].lexeme.substring(1) );
    	Integer i = Integer.valueOf( tokens[currentToken + 3].toInt() );

       	Number = (OpCode << 26 ) | ( S << 21) | ( T << 16 ) | ( i & 0xffff );
    	toScreen( Number );
    }
    
    // Assemble and shift opcode
    private void AssembleOpcode( Token[] tokens, int currentToken, int lineNumber ){
    	String temp = tokens[currentToken].lexeme.toUpperCase();
    	OpCode operator = OpCode.valueOf( temp );
    	Integer Number = 0;
    	
    	switch( operator ) {
    		case JR:
    			codeJR ( tokens, currentToken );
    			break;
    		case JALR:
    			codeJALR ( tokens, currentToken );
    			break;
    		case ADD:
    			Number = 0x20;
    			codeSimpleR ( tokens, currentToken, Number );
    			break;
    		case SUB:
    			Number = 0x22;
    			codeSimpleR ( tokens, currentToken, Number );
    			break;
    		case SLT:
    			Number = 0x2A;
    			codeSimpleR ( tokens, currentToken, Number );
    			break;
    		case SLTU:
    			Number = 0x2B;
    			codeSimpleR ( tokens, currentToken, Number );
    			break;
    		case BEQ:
    			Number = 4;
    			codeBranches ( tokens, currentToken, Number, lineNumber );
    			break;
    		case BNE:
    			Number = 5;
    			codeBranches ( tokens, currentToken, Number, lineNumber );
    			break;
    		case MFHI:
    			Number = 0x10;
    			codeMoves ( tokens, currentToken, Number );
    			break;
    		case MFLO:
    			Number = 0x12;
    			codeMoves ( tokens, currentToken, Number );
    			break;
    		case LIS:
    			Number = 0x14;
    			codeMoves ( tokens, currentToken, Number );
    			break;
    		case MULT:
    			Number = 0x18;
    			codeMulDiv ( tokens, currentToken, Number );
    			break;
    		case DIV:
    			Number = 0x1A;
    			codeMulDiv ( tokens, currentToken, Number );
    			break;
    		case MULTU:
    			Number = 0x19;
    			codeMulDiv ( tokens, currentToken, Number );
    			break;
    		case DIVU:
    			Number = 0x1B;
    			codeMulDiv ( tokens, currentToken, Number );
    			break;
    		case LW:
    			Number = 35;
    			codeSLWord( tokens, currentToken, Number );
    			break;
    		case SW:
    			Number = 43;
    			codeSLWord( tokens, currentToken, Number );
    			break;
    	}
    }
    
    // Sort out what opcode it is
    private void sortOpcode( Token[] tokens, int currentToken ){
    	
    	// convert String to ENUM
    	String temp = tokens[currentToken].lexeme.toUpperCase();
    	
    	OpCode operator = OpCode.BLANK;
        try {
        	operator = OpCode.valueOf( temp );
        } catch ( Exception e) {
        	System.err.println("ERROR, the opCode " + temp + " does not exist");
        	System.exit(0);
        }
    	
    	switch( operator ) {
    	
    		case JR:
    		case JALR:
    			jump ( tokens, currentToken );
    			break;
    		case ADD:
    		case SUB:
    		case SLT:
    		case SLTU:
    			simpleR( tokens, currentToken );
    			break;
    		case BEQ:
    		case BNE:
    			branches( tokens, currentToken);
    			break;
    		case MFHI:
    		case MFLO:
    		case LIS:
    			moves ( tokens, currentToken);
    			break;
    		case MULT:
    		case MULTU:
    		case DIV:
    		case DIVU:
    			mulDiv ( tokens, currentToken );
    			break;
    		case LW:
    		case SW:
    			slWords( tokens, currentToken );
    			break;
    		default:
    			System.err.println( "ERROR, Invalid OpCode" );
    			System.exit(0);
    	}
    }
    
    // input should be either System.in or a FileInputStream attached to an input file (something.asm).
    private String run( InputStream input ) {

        Lexer   lexer = new Lexer();
        Scanner in    = new Scanner( input );
        int locCounter = 0;
        List<Token[]> listl = new ArrayList<Token[]>();
        
        while( in.hasNextLine() ) {
            
            String line = in.nextLine();
            // Scan the line into an array of tokens.
            Token[] tokens;
            tokens = lexer.scan( line );
        	int currentToken = 0; //current token
        	listl.add( tokens );
            
            //pass 1, get all labels in to symbol table
            if (tokens.length == 0 ){
            	
            } else {
            	while ( currentToken < tokens.length && tokens[currentToken].kind == Kind.LABEL ){
                	String label = tokens[currentToken].lexeme.substring( 0, ( tokens[currentToken].lexeme.length() - 1 ) ); //get rid of :
                	makeLabel( label, locCounter );
                	currentToken++;
            	}
            }
            
            if ( currentToken == tokens.length ){
            	
            } else {
            	
            	Kind tempKind = tokens[currentToken].kind;
            	
            	switch ( tempKind ){
            	
            		case DOTWORD: {
            			dotWord( tokens, currentToken );
                        locCounter += 4;
            			break;
            		}
            		case WHITESPACE: {
            			break;
            		}
            		case ID: {
            			sortOpcode( tokens, currentToken );
            			locCounter += 4;
            			break;
            		}
            		default: {
            			System.err.println( "ERROR, invalid mips assembly code" );
            			System.exit(0);
            		}
            	}

            }

	            
	            System.err.println( line );
	            for( int i = 0; i < tokens.length; i++ ) {
	                System.err.println( "  Token: " + tokens[i] );
	            }
            }
        
        // 2nd pass
        // loop through array list, looking for certain keywords.
        int i = 0;
    	int current = 0;
        int lineCounter = 0;
        while  ( i < listl.size() ) {
        	Token[] tempTokens = listl.get(i); 
        	current = 0;
        	// iterates past all labels
            if (tempTokens.length == 0 ){
            } else {
            	while ( current < tempTokens.length && tempTokens[current].kind == Kind.LABEL ){
                	current++;
            	}
            }
    	// Dotword
            if ( current == tempTokens.length ){
            	
            } else {
	    		if ( tempTokens[current].kind == Kind.DOTWORD ){
	    			lineCounter += 4;
	    			if ( tempTokens[current+1].kind == Kind.HEXINT || tempTokens[current+1].kind == Kind.INT ){
	    				int Numbers = tempTokens[current+1].toInt();
	    				toScreen( Numbers );
	    			} else {
	    				Integer Number = symbolTable.get( tempTokens[current+1].lexeme );
	    				if ( Number == null ){
	    					System.err.println("ERROR, The label " + tempTokens[current+1].lexeme + " is not defined" );
	    					System.exit(0);
	    				}
	    				toScreen( Number );
	    			}
	    		} else if ( tempTokens[current].kind == Kind.ID) {
	    			lineCounter += 4;
	    			AssembleOpcode( tempTokens, current, lineCounter);
	    		}
            }
    	i++;
        }
        
//         print symbol table
        for ( String value : symbolTable.keySet() ) {
        	System.err.println( value + " " + symbolTable.get( value ) ); 
        }
        
        System.out.flush();

        // Main ignores the value returned, but the "OK" is useful if you decide to to JUnit testing;
        // run should return either a string containing "ERROR" or a string containing "OK", depending
        // on whether or not your assembler finds an error in the file it's assembly. Of course, that
        // leaves open the question of whether the MIPS code generated for a program w/o syntax errors
        // is semantically correct. You can automate testing that, too, but it takes more work since
        // you have to run the resulting *.mips file via java cs241.twoints and check its output...
        return( "OK" );
    }
}

/** The various kinds of tokens (ie values of Token.kind). */
enum Kind {
    ID,             // Opcode or identifier (use of a label)
    INT,            // Decimal integer
    HEXINT,         // Hexadecimal integer
    REGISTER,       // Register number
    COMMA,          // Comma
    LPAREN,         // (
    RPAREN,         // )
    LABEL,          // Declaration of a label (with a colon)
    DOTWORD,        // .word directive
    WHITESPACE;     // Whitespace
}


/** The various kinds of opcodes that are legal */
enum OpCode {
	BLANK,
    ADD, 			
    SUB,
    MULT,
    MULTU,
    DIV,
    DIVU,
    MFHI,
    MFLO,
    LIS,
    LW,
    SW,
    SLT,
    SLTU,
    BEQ,
    BNE,
    JR,
    JALR;
}

/** The representation of a token. */
class Token {
    
    public Kind   kind;   // The kind of token.
    public String lexeme; // String representation of the actual token in the source code.

    public Token( Kind kind, String lexeme ) {
        this.kind   = kind;
        this.lexeme = lexeme;
    }

    public String toString() {
        return kind + " {" + lexeme + "}";
    }

    /** Returns an integer representation of the token. For tokens of kind
     *  INT (decimal integer constant) and HEXINT (hexadecimal integer
     *  constant), returns the integer constant. For tokens of kind
     *  REGISTER, returns the register number.
     */
    public int toInt() {
        if(      kind == Kind.INT      ) return parseLiteral( lexeme,              10, 32 );
        else if( kind == Kind.HEXINT   ) return parseLiteral( lexeme.substring(2), 16, 32 );
        else if( kind == Kind.REGISTER ) return parseLiteral( lexeme.substring(1), 10,  5 );
        else {
            System.err.println( "ERROR in to-int conversion." );
            System.exit(1);
            return 0;
        }
    }
    
    private int parseLiteral( String s, int base, int bits ) {
        BigInteger x = new BigInteger( s, base );
        if( x.signum() > 0 ) {
            if( x.bitLength() > bits ) {
                System.err.println( "ERROR in parsing: constant out of range: " + s );
                System.exit(1);
            }
        } else if( x.signum() < 0 ) {
            if( x.negate().bitLength() > bits-1
                    && x.negate().subtract(new BigInteger("1")).bitLength() > bits-1 ) {
                System.err.println( "ERROR in parsing: constant out of range: " + s );
                System.exit(1);
            }
        }
        return (int) (x.longValue() & ((1L << bits) - 1));
    }
}

// Lexer -- implements a DFA that partitions an input line into a list of tokens.
// DFAs will be discussed Lectures 10, 11 and 12 and Assignment 5.
class Lexer {

    public Lexer() {
        
        CharSet whitespace    = new Chars( "\t\n\r " );
        CharSet letters       = new Chars( "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"           );
        CharSet lettersDigits = new Chars( "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789" );
        CharSet digits        = new Chars( "0123456789"                                                     );
        CharSet hexDigits     = new Chars( "0123456789ABCDEFabcdef"                                         );
        CharSet oneToNine     = new Chars( "123456789"                                                      );
        CharSet all           = new AllChars();

        /** The handling of whitespace is tricky. There are two things you should figure out:
         *  (a) how and why all of the characters following // are swallowed up w/o returning a token;
         *  (b) how the appearance of one or more whitespace characters causes this Lexer to cease
         *      building up an ID or keyword, which is then appended to the list of tokens found in
         *      the line, and start scanning for another token.      
        **/

        table = new Transition[] {
                new Transition( State.START,    whitespace,     State.WHITESPACE ),
                new Transition( State.START,    letters,        State.ID         ),
                new Transition( State.ID,       lettersDigits,  State.ID         ),
                new Transition( State.START,    oneToNine,      State.INT        ),
                new Transition( State.INT,      digits,         State.INT        ),
                new Transition( State.START,    new Chars("-"), State.MINUS      ),
                new Transition( State.MINUS,    digits,         State.INT        ),
                new Transition( State.START,    new Chars(","), State.COMMA      ),
                new Transition( State.START,    new Chars("("), State.LPAREN     ),
                new Transition( State.START,    new Chars(")"), State.RPAREN     ),
                new Transition( State.START,    new Chars("$"), State.DOLLAR     ),
                new Transition( State.DOLLAR,   digits,         State.REGISTER   ),
                new Transition( State.REGISTER, digits,         State.REGISTER   ),
                new Transition( State.START,    new Chars("0"), State.ZERO       ),
                new Transition( State.ZERO,     new Chars("x"), State.ZEROX      ),
                new Transition( State.ZERO,     digits,         State.INT        ),
                new Transition( State.ZEROX,    hexDigits,      State.HEXINT     ),
                new Transition( State.HEXINT,   hexDigits,      State.HEXINT     ),
                new Transition( State.ID,       new Chars(":"), State.LABEL      ),
                new Transition( State.START,    new Chars(";"), State.COMMENT    ),
                new Transition( State.START,    new Chars("."), State.DOT        ),
                new Transition( State.DOT,      new Chars("w"), State.DOTW       ),
                new Transition( State.DOTW,     new Chars("o"), State.DOTWO      ),
                new Transition( State.DOTWO,    new Chars("r"), State.DOTWOR     ),
                new Transition( State.DOTWOR,   new Chars("d"), State.DOTWORD    ),
                new Transition( State.COMMENT,  all,            State.COMMENT    )
        };
    }

    /** Partitions the line passed in as input into an array of tokens.
     *  The array of tokens is returned.
     */
    public Token[] scan( String input ) {

        List<Token> ret = new ArrayList<Token>();

        if( input.length() == 0 ) return new Token[0];
        int   i          = 0;
        int   startIndex = 0;
        State state      = State.START;

        while( true ) {

            Transition trans = null;

            if( i < input.length() ) trans = findTransition( state, input.charAt(i) );
            
            if( trans == null ) {
                // No more transitions possible
                if( ! state.isFinal() ) {
                    System.err.println( "ERROR in lexing after reading " + input.substring(0,i) );
                    System.exit(1);
                }
                if( state.kind != Kind.WHITESPACE ) {
                    ret.add( new Token(state.kind,input.substring(startIndex,i)) );
                }
                startIndex = i;
                state      = State.START;
                if( i >= input.length() ) break;
            } else {
                state      = trans.toState;
                i++;
            }
        }
        
        return ret.toArray( new Token[ret.size()] );
    }

    ///////////////////////////////////////////////////////////////
    // END OF PUBLIC METHODS
    ///////////////////////////////////////////////////////////////

    private Transition findTransition( State state, char c ) {
        for( int j = 0; j < table.length; j++ ) {
            Transition trans = table[j];
            if( trans.fromState == state && trans.chars.contains(c) ) {
                return trans;
            }
        }
        return null;
    }

    // Final states or those whose kind (of token) is not null, except for WHITESPACE (a special case).
    private static enum State {
        START(      null            ),
        DOLLAR(     null            ),
        MINUS(      null            ),
        REGISTER(   Kind.REGISTER   ),
        INT(        Kind.INT        ),
        ID(         Kind.ID         ),
        LABEL(      Kind.LABEL      ),
        COMMA(      Kind.COMMA      ),
        LPAREN(     Kind.LPAREN     ),
        RPAREN(     Kind.RPAREN     ),
        ZERO(       Kind.INT        ),
        ZEROX(      null            ),
        HEXINT(     Kind.HEXINT     ),
        COMMENT(    Kind.WHITESPACE ),
        DOT(        null            ),
        DOTW(       null            ),
        DOTWO(      null            ),
        DOTWOR(     null            ),
        DOTWORD(    Kind.DOTWORD    ),
        WHITESPACE( Kind.WHITESPACE );

        Kind kind;

        State( Kind kind ) {
            this.kind = kind;
        }

        boolean isFinal() {
            return kind != null;
        }
    }

    private interface CharSet {
        public boolean contains( char newC );
    }

    private class Chars implements CharSet {
        private String chars;
        public  Chars( String chars ) { this.chars = chars; }
        public  boolean contains( char newC ) {
            return chars.indexOf(newC) >= 0;
        }
    }

    private class AllChars implements CharSet {
        public boolean contains( char newC ) {
            return true;
        }
    }

    private class Transition {
        State   fromState;
        CharSet chars;
        State   toState;
        Transition( State fromState, CharSet chars, State toState ) {
            this.fromState = fromState;
            this.chars     = chars;
            this.toState   = toState;
        }
    }
    
    private Transition[] table;
}
