package com.littlehorizons.erp;

import com.littlehorizons.erp.controller.MainController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.view.MainView;
import com.littlehorizons.erp.view.StudentView;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class Main {

    public static void main(String[] args) {
        configureConsoleEncoding();

        ConsoleInput input = new ConsoleInput();
        StudentController studentController =
                new StudentController(new StudentService(), new StudentView(input));
        MainController mainController =
                new MainController(new MainView(input), studentController);

        mainController.run();
        System.out.println("Hasta pronto.");
    }

    /**
     * Makes the console print accents and special characters correctly (UTF-8).
     */
    private static void configureConsoleEncoding() {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
    }
}