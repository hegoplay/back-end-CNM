package iuh.fit.se.util;

public class FormatUtils {
	public static String formatPhoneNumber(String phone) {
        phone = phone.trim().replaceAll("\\s+", "");
        if (!phone.startsWith("+")) {
            phone = "+" + phone;
        }
        return phone;
    }
}	
