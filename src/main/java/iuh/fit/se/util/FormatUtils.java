package iuh.fit.se.util;

public class FormatUtils {
	public static String formatPhoneNumber(String phone) {
        phone = phone.trim().replaceAll("\\s+", "");
//      if first character is 0, remove it
        if (phone.startsWith("0")) {
			phone = phone.substring(1);
			phone = "+84" + phone;
		}
        if (!phone.startsWith("+")) {
            phone = "+" + phone;
        }
        return phone;
    }
}	
