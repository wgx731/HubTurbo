package tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import filter.ParseException;
import filter.Parser;
import filter.expression.Conjunction;
import filter.expression.DateRange;
import filter.expression.Disjunction;
import filter.expression.Negation;
import filter.expression.NumberRange;
import filter.expression.Qualifier;
import filter.lexer.Lexer;
import filter.lexer.Token;
import filter.lexer.TokenType;

public class FilterParserTests {

    @Test
    public void basics() {
        assertEquals(Parser.parse(null), null);
        assertEquals(Parser.parse(""), Qualifier.EMPTY);
    }
    
    @Test
    public void keywords() {
        assertEquals(Parser.parse("a(b)"), new Conjunction(new Qualifier("keyword", "a"), new Qualifier("keyword", "b")));
        assertEquals(Parser.parse("    a   (   b   )   "), new Conjunction(new Qualifier("keyword", "a"), new Qualifier("keyword", "b")));
        assertEquals(Parser.parse("a(b c)"), new Conjunction(new Qualifier("keyword", "a"), new Conjunction(new Qualifier("keyword", "b"), new Qualifier("keyword", "c"))));
        assertEquals(Parser.parse("c a(b)"), new Conjunction(new Conjunction(new Qualifier("keyword", "c"), new Qualifier("keyword", "a")), new Qualifier("keyword", "b")));
    }
    
    @Test
    public void quotes() {
    	// Quoted qualifier content
    	assertEquals(Parser.parse("created:\"a b\""), new Qualifier("created", "a b"));
    	assertEquals(Parser.parse("created:\" > 2014-5-1 \""), new Qualifier("created", new DateRange(LocalDate.of(2014, 5, 1), null, true)));
    	assertEquals(Parser.parse("created:\" 2014-5-1 .. 2014-5-2 \""), new Qualifier("created", new DateRange(LocalDate.of(2014, 5, 1), LocalDate.of(2014, 5, 2))));
    	
    	// Prefix quotes
    	assertEquals(Parser.parse("\"a b\""), new Qualifier("keyword", "a b"));
    }
    
    @Test
    public void unexpectedEOFs() {
        try {
            Parser.parse("~");
            fail("Inputs which end unexpectedly should throw a parse exception");
        } catch (ParseException e) {}
        try {
            Parser.parse("a(b) ||");
            fail("Inputs which end unexpectedly should throw a parse exception");
        } catch (ParseException e) {}
    }
    
    @Test
    public void emptyQualifiers() {
    	
        try {
        	Parser.parse("-milestone: label:priority.high");
            fail("'label:' can't be the input to a qualifier => empty qualifier => parse exception");
        } catch (ParseException e) {}
    	
        try {
        	Parser.parse("label:priority.high -milestone:");
            fail("Empty qualifiers should cause a parse exception");
        } catch (ParseException e) {}
    }
    
    @Test
    public void operators() {
        assertEquals(Parser.parse("a:b OR c:d"), new Disjunction(new Qualifier("a", "b"), new Qualifier("c", "d")));
        assertEquals(Parser.parse("a:b | c:d"), new Disjunction(new Qualifier("a", "b"), new Qualifier("c", "d")));
        assertEquals(Parser.parse("a:b || c:d"), new Disjunction(new Qualifier("a", "b"), new Qualifier("c", "d")));

        assertEquals(Parser.parse("a:b c:d"), new Conjunction(new Qualifier("a", "b"), new Qualifier("c", "d")));
        assertEquals(Parser.parse("a:b AND c:d"), new Conjunction(new Qualifier("a", "b"), new Qualifier("c", "d")));
        assertEquals(Parser.parse("a:b & c:d"), new Conjunction(new Qualifier("a", "b"), new Qualifier("c", "d")));
        assertEquals(Parser.parse("a:b && c:d"), new Conjunction(new Qualifier("a", "b"), new Qualifier("c", "d")));

        assertEquals(Parser.parse("!a:b"), new Negation(new Qualifier("a", "b")));
        assertEquals(Parser.parse("-a:b"), new Negation(new Qualifier("a", "b")));
        assertEquals(Parser.parse("~a:b"), new Negation(new Qualifier("a", "b")));
        
        // Implicit conjunction
        
        assertEquals(Parser.parse("milestone:0.4 state:open OR label:urgent"),
                new Disjunction(new Conjunction(new Qualifier("milestone", "0.4"), new Qualifier("state", "open")), new Qualifier("label", "urgent")));
        assertEquals(Parser.parse("milestone:0.4 state:open OR label:urgent"), Parser.parse("milestone:0.4 AND state:open OR label:urgent"));
    }
    
    @Test
    public void associativity() {
        assertEquals(Parser.parse("a:b OR c:d OR e:f"),
                new Disjunction(new Disjunction(new Qualifier("a", "b"), new Qualifier("c", "d")), new Qualifier("e", "f")));

        assertEquals(Parser.parse("a:b AND c:d AND e:f"),
                new Conjunction(new Conjunction(new Qualifier("a", "b"), new Qualifier("c", "d")), new Qualifier("e", "f")));
    }

    @Test
    public void numberOperators() {
        assertEquals(Parser.parse("updated:<24"),
        		new Qualifier("updated", new NumberRange(null, 24, true)));
        assertEquals(Parser.parse("updated:<=24"),
        		new Qualifier("updated", new NumberRange(null, 24)));
        assertEquals(Parser.parse("updated:>=24"),
        		new Qualifier("updated", new NumberRange(24, null)));
        assertEquals(Parser.parse("updated:>24"),
        		new Qualifier("updated", new NumberRange(24, null, true)));

        assertEquals(Parser.parse("updated:24"),
        		new Qualifier("updated", new NumberRange(null, 24, true)));
        assertEquals(Parser.parse("updated:<24"),
        		Parser.parse("updated:24"));
    }

    @Test
    public void numberRanges() {
    	assertEquals(Parser.parse("updated:1 .. 24"),
    			new Qualifier("updated", new NumberRange(1, 24, true)));
    	assertEquals(Parser.parse("updated:1 .. *"),
    			new Qualifier("updated", new NumberRange(1, null)));

    	// Parsing currently requires a space between operand and operator
    	assertEquals(Parser.parse("updated:1..2"),
    			new Qualifier("updated", "1..2"));
	}

    @Test
    public void dateRanges() {
        assertEquals(Parser.parse("created:2014-06-01 .. 2013-03-15"),
                new Qualifier("created", new DateRange(LocalDate.of(2014, 06, 01), LocalDate.of(2013, 03, 15))));

        assertEquals(Parser.parse("created:2014-06-01 .. *"),
                new Qualifier("created", new DateRange(LocalDate.of(2014, 06, 01), null)));

        assertEquals(Parser.parse("a created:2014-06-01 .. 2013-03-15 b"),
        		new Conjunction(
        				new Conjunction(
        						new Qualifier("keyword", "a"),
        						new Qualifier("created", new DateRange(LocalDate.of(2014, 06, 01), LocalDate.of(2013, 03, 15)))),
        				new Qualifier("keyword", "b"))
        );
    }

    @Test
    public void dateRangeUsage() {
    	
    	// date .. date
    	DateRange dr = new DateRange(LocalDate.of(2014, 1, 1), LocalDate.of(2014, 6, 3));
    	assertEquals(dr.encloses(LocalDate.of(2014, 1, 1)), true);
    	assertEquals(dr.encloses(LocalDate.of(2014, 6, 3)), true);
    	assertEquals(dr.encloses(LocalDate.of(2014, 2, 2)), true);
    	assertEquals(dr.encloses(LocalDate.of(2014, 7, 1)), false);

    	// date .. date
    	// strict inequality
    	dr = new DateRange(LocalDate.of(2014, 1, 1), LocalDate.of(2014, 6, 3), true);
    	assertEquals(dr.encloses(LocalDate.of(2014, 1, 1)), false);
    	assertEquals(dr.encloses(LocalDate.of(2014, 6, 3)), false);
    	assertEquals(dr.encloses(LocalDate.of(2014, 2, 2)), true);
    	assertEquals(dr.encloses(LocalDate.of(2014, 7, 1)), false);

    	// date .. *
    	dr = new DateRange(LocalDate.of(2014, 1, 1), null);
    	assertEquals(dr.encloses(LocalDate.of(2014, 1, 1)), true);
    	assertEquals(dr.encloses(LocalDate.of(2014, 6, 3)), true);
    	assertEquals(dr.encloses(LocalDate.of(2013, 2, 2)), false);

    	// date .. *
    	// strict inequality
    	dr = new DateRange(LocalDate.of(2014, 1, 1), null, true);
    	assertEquals(dr.encloses(LocalDate.of(2014, 1, 1)), false);
    	assertEquals(dr.encloses(LocalDate.of(2014, 6, 3)), true);
    	assertEquals(dr.encloses(LocalDate.of(2013, 2, 2)), false);

    	// * .. date
    	dr = new DateRange(null, LocalDate.of(2014, 1, 1));
    	assertEquals(dr.encloses(LocalDate.of(2014, 1, 1)), true);
    	assertEquals(dr.encloses(LocalDate.of(2014, 6, 3)), false);
    	assertEquals(dr.encloses(LocalDate.of(2013, 2, 2)), true);

    	// * .. date
    	// strict inequality
    	dr = new DateRange(null, LocalDate.of(2014, 1, 1), true);
    	assertEquals(dr.encloses(LocalDate.of(2014, 1, 1)), false);
    	assertEquals(dr.encloses(LocalDate.of(2014, 6, 3)), false);
    	assertEquals(dr.encloses(LocalDate.of(2013, 2, 2)), true);
    }
    
    @Test
    public void dateRangeOperators() {
        assertEquals(Parser.parse("created:<2014-06-01"),
                new Qualifier("created", new DateRange(null, LocalDate.of(2014, 6, 1), true)));
        
        assertEquals(Parser.parse("created : <= 2014-06-01"),
                new Qualifier("created", new DateRange(null, LocalDate.of(2014, 6, 1))));

        assertEquals(Parser.parse("created : > 2014-06-01"),
                new Qualifier("created", new DateRange(LocalDate.of(2014, 6, 1), null, true)));
        
        assertEquals(Parser.parse("created : >= 2014-06-01"),
                new Qualifier("created", new DateRange(LocalDate.of(2014, 6, 1), null)));
    }
    
    @Test
    public void dates() {
        assertEquals(Parser.parse("created   :   2014-6-1"),
                new Qualifier("created", LocalDate.of(2014, 6, 1)));

        assertEquals(Parser.parse("created   :   2014-06-01"),
                new Qualifier("created", LocalDate.of(2014, 6, 1)));
        
        assertEquals(Parser.parse("a created   :   2014-06-01 b"),
        		new Conjunction(
        				new Conjunction(
        						new Qualifier("keyword", "a"),
        						new Qualifier("created", LocalDate.of(2014, 06, 01))),
						new Qualifier("keyword", "b")));
    }

    @Test
    public void precedence() {
        assertEquals(Parser.parse("a:b OR c:d AND e:f"),
                new Disjunction(new Qualifier("a", "b"), new Conjunction(new Qualifier("c", "d"), new Qualifier("e", "f"))));
        assertEquals(Parser.parse("~a:b OR c:d AND e:f"),
                new Disjunction(new Negation(new Qualifier("a", "b")), new Conjunction(new Qualifier("c", "d"), new Qualifier("e", "f"))));
        
        assertEquals(Parser.parse("a:b ~c:d"), new Conjunction(new Qualifier("a", "b"), new Negation(new Qualifier("c", "d"))));
    }

    @Test
    public void grouping() {
        assertEquals(Parser.parse("(a:b OR c:d) AND e:f"),
                new Conjunction(new Disjunction(new Qualifier("a", "b"), new Qualifier("c", "d")), new Qualifier("e", "f")));
        assertEquals(Parser.parse("(a:b OR c:d) e:f"),
                new Conjunction(new Disjunction(new Qualifier("a", "b"), new Qualifier("c", "d")), new Qualifier("e", "f")));
        assertEquals(Parser.parse("e:f ~(a:b OR c:d)"),
                new Conjunction(new Qualifier("e", "f"), new Negation(new Disjunction(new Qualifier("a", "b"), new Qualifier("c", "d")))));
    }
    
    @Test
    public void colon() {
        assertEquals(Parser.parse("assignee:darius"),
                new Qualifier("assignee", "darius"));
        assertEquals(Parser.parse("assignee    :    darius   "),
                new Qualifier("assignee", "darius"));
        assertEquals(Parser.parse("assignee:dar ius(one)"),
                new Conjunction(new Conjunction(new Qualifier("assignee", "dar"), new Qualifier("keyword", "ius")), new Qualifier("keyword", "one")));
    }
    
    @Test
    public void lexer() {
        assertEquals(new Lexer("").lex(), new ArrayList<Token>(Arrays.asList(
                new Token(TokenType.EOF, "", 0))));
        assertEquals(new Lexer("a' b' c'").lex(), new ArrayList<Token>(Arrays.asList(
                new Token(TokenType.SYMBOL, "a'", 0),
                new Token(TokenType.SYMBOL, "b'", 0),
                new Token(TokenType.SYMBOL, "c'", 0),
                new Token(TokenType.EOF, "", 0))));
    }
    
    @Test
    public void serialisation() {
    	
    	String[] tests = {
    		"abcdefg:hijkl",
    		"!abcdefg:hijkl", // NOT
    		"abcdefg:hijkl || zxc:aksljd", // OR
    		"abcdefg:hijkl && zxc:aksljd", // AND
    		"abcdefg:hijkl && zxc:aksljd || alsk:asl", // OR and AND precedence
    		"abcdefg:hijkl || zxc:aksljd && alsk:asl",
    		"(abcdefg:hijkl || zxc:aksljd) && alsk:asl", // Grouping
    		"(abcdefg:hijkl && zxc:aksljd) || alsk:asl",
    		"abcdefg:hijkl && !zxc:aksljd || !alsk:asl", // OR, AND, NOT
    		"abcdefg:hijkl || !zxc:aksljd && !alsk:asl",
			"updated:>24", // Number ranges
			"updated:>=24",
			"updated:<24",
			"updated:<=24",
			"created:<=2014-12-4", // Date operators
			"created:>=2014-12-4",
			"created:<2014-12-4",
			"created:>2014-12-4",
			"created:2014-12-4 .. 2014-12-6", // Date ranges
    	};

    	// We want to ensure that parsing some filter, and parsing the serialised version
    	// of that filter, results in the same data structure.
    	for (int i=0; i<tests.length; i++) {
        	assertEquals(Parser.parse(Parser.parse(tests[i]).toString()), Parser.parse(tests[i]));
    	}
    }
}
