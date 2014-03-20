package fr.w3blog.mockimail;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.text.ParseException;

public class TestDate {

	public static void main(String arg[]) throws ParseException {

		String s = "C'�tait cool";
		CharsetEncoder encoder = Charset.forName("iso-8859-1").newEncoder();
		System.out.println(encoder.canEncode(s));

		CharsetEncoder encoderUtf = Charset.forName("UTF-8").newEncoder();
		System.out.println(encoderUtf.canEncode(s));
		System.out.println(s);

		System.out.println(new String(s.getBytes(), Charset.forName("iso-8859-1")));

	}
}
