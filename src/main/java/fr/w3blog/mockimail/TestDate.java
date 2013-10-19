package fr.w3blog.mockimail;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TestDate {

	public static void main(String arg[]) throws ParseException {
		String d = "Sat, 19 Oct 2013 19:57:50 +0200";
		SimpleDateFormat sdf = new SimpleDateFormat(
				"EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
		System.out.println(sdf.format(new Date()));
		System.out.println(sdf.parse(d));

	}
}
