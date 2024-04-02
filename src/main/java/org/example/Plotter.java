package org.example;

import java.util.Random;
import org.jzy3d.chart.Chart;
import org.jzy3d.chart.EmulGLSkin;
import org.jzy3d.chart.factories.EmulGLChartFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.colors.ColorMapper;
import org.jzy3d.colors.colormaps.ColorMapRainbow;
import org.jzy3d.maths.Range;
import org.jzy3d.plot3d.builder.Mapper;
import org.jzy3d.plot3d.builder.SurfaceBuilder;
import org.jzy3d.plot3d.builder.concrete.OrthonormalGrid;
import org.jzy3d.plot3d.primitives.Scatter;
import org.jzy3d.plot3d.primitives.Shape;
import org.jzy3d.plot3d.rendering.canvas.Quality;

public class Plotter {
    public Scatter scatter;
    public Chart chart;
    public Plotter(FunctionParser function, Range xRange, Range yRange) {

        Quality q = Quality.Advanced();

        chart = new EmulGLChartFactory().newChart(q);
        chart.add(drawFunction(function, xRange, yRange));

        chart.open();
        chart.addMouse();

        EmulGLSkin skin = EmulGLSkin.on(chart);
        skin.getCanvas().setProfileDisplayMethod(true);
    }

    private static Shape drawFunction(FunctionParser function, Range xRange , Range yRange) {

        Mapper mapper = new Mapper(){
            public double f(double x, double y) {
                function.getFunction().setArgumentValue(0, x);
                function.getFunction().setArgumentValue(1, y);
                return function.getFunction().calculate();
            }
        };

        int steps = 80;
        Shape surface = new SurfaceBuilder().orthonormal(new OrthonormalGrid(xRange, steps, yRange, steps), mapper);
        surface.setColorMapper(new ColorMapper(new ColorMapRainbow(), surface, new Color(1, 1, 1, .5f)));
        surface.setFaceDisplayed(true);
        surface.setWireframeDisplayed(true);
        surface.setWireframeColor(Color.BLACK);
        return surface;
    }
}