package com.example.community;

import java.io.IOException;

public class WKTests {

    public static void main(String[] args) {
        String cmd = "E:/maven/wkhtmltopdf/bin/wkhtmltoimage --quality 75  https://www.nowcoder.com  E:/maven/data/wk-images/3.png";
        try {
            Runtime.getRuntime().exec(cmd);
            System.out.println("ok.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
