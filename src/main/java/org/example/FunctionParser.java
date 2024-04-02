package org.example;

import org.mariuszgromada.math.mxparser.Function;
import org.mariuszgromada.math.mxparser.License;

import java.util.Scanner;

public class FunctionParser {

    private final Function function;
    public FunctionParser() {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter a function definition: ");
        String expressionString = scanner.nextLine();

        License.iConfirmNonCommercialUse("Sebastian Rogaczewski");

        this.function = new Function(expressionString);
    }

    public Function getFunction() {
        return function;
    }
}
