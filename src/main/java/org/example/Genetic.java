package org.example;

import org.jzy3d.colors.Color;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.maths.Range;
import org.jzy3d.plot3d.primitives.Scatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Genetic {

    private static final int POPULATION_SIZE = 50;
    private static final int MAX_GENERATIONS = 40;
    private static final double MUTATION_RATE = 0.05;
    private static final int SLEEP_VALUE = 200;
    private static FunctionParser function;
    private static Plotter plot;

    public static void main(String[] args) {

        //Rosenbrock: f(x,y)=(1-x)^2+100*(y-x*x)^2
        //Rastrigin: f(x,y)=10*2+((x^2-10cos(2*π*x))+(y^2-10cos(2*π*y)))
        //Sphere: f(x,y)=x^2+y^2
        //f(x,y)=sin(x^2+y^2)/(abs(x*y)+1)
        function = new FunctionParser();
        if (!function.getFunction().checkSyntax()) {
            System.out.println("Incorrect function!");
            System.out.println("Definition should start with <function_name(x, y)=...>");
            System.exit(1);
        }

        Range xRange = new Range(-2, 2);
        Range yRange = new Range(-1, 3);

        try {
            plot = new Plotter(function, xRange, yRange);

            Coord3d[] finalPopulation = run(xRange, yRange);

            System.out.println("Average z value: " + findAverage(finalPopulation));
            System.out.println("Minimum z value: " + findMin(finalPopulation));

            plot.scatter.setColor(Color.RED);
            plot.chart.render();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Coord3d[] run(Range xRange, Range yRange) throws InterruptedException {
        Coord3d[] population = generatePopulation(xRange.getMin(), xRange.getMax(), yRange.getMin(), yRange.getMax());

        plot.scatter = new Scatter(population, Color.BLUE);
        plot.scatter.setWidth(30);
        plot.chart.add(plot.scatter);
        Thread.sleep(SLEEP_VALUE);

        for (int i = 0; i < MAX_GENERATIONS; i++)
        {
            List<Float> fitnessValues = new ArrayList<>();
            for (Coord3d chromosome : population)
            {
                fitnessValues.add(chromosome.z);
            }

            List<Float> rouletteWheel = proportionalSelection(fitnessValues);

            Coord3d[] parents1 = getParents(rouletteWheel, population);
            Coord3d[] parents2 = getParents(rouletteWheel, population);

            population = getNewPopulation(parents1, parents2);

            plot.scatter.setData(population);
            plot.chart.render();
            Thread.sleep(SLEEP_VALUE);
        }

        return population;
    }

    private static Coord3d[] getNewPopulation(Coord3d[] parents1, Coord3d[] parents2) {
        Coord3d[] newPopulation = new Coord3d[parents1.length];
        Random random = new Random();
        boolean mutate = false;

        for (int i = 0; i < parents1.length; i++)
        {
            if (random.nextFloat() < MUTATION_RATE) {
                mutate = true;
            }

            newPopulation[i] = crossover(parents1[i], parents2[i], mutate);

            mutate = false;
        }

        return newPopulation;
    }

    static Coord3d crossover(Coord3d parent1, Coord3d parent2, boolean mutate) {
        Coord3d newChromosome = new Coord3d();

        Random random = new Random();

        float alpha = random.nextFloat();
        float beta = random.nextFloat();

        //"blending crossover" - weighted average of all the elements (here x and y)
        newChromosome.x = (alpha * parent1.x + (1 - alpha) * parent2.x);
        newChromosome.y = (beta * parent1.y + (1 - beta) * parent2.y);

        if (mutate) {
            float maxMutation = (float)0.1;
            float minMutation = (float)0.0001;
            int add_or_sub = random.nextInt(2);

            float mutation = minMutation + random.nextFloat() * (maxMutation - minMutation);
            if (add_or_sub == 0) newChromosome.x += mutation;
            else newChromosome.x -= mutation;

            mutation = minMutation + random.nextFloat() * (maxMutation - minMutation);
            if (add_or_sub == 0) newChromosome.y += mutation;
            else newChromosome.y -= mutation;
        }

        function.getFunction().setArgumentValue(0, newChromosome.x);
        function.getFunction().setArgumentValue(1, newChromosome.y);
        newChromosome.z = (float)function.getFunction().calculate();

        return newChromosome;
    }

    private static Coord3d[] getParents(List<Float> rouletteWheel, Coord3d[] population) {

        Coord3d[] parents = new Coord3d[population.length];
        Random random = new Random();
        for (int i = 0; i < population.length; i++)
        {
            double randomValue = random.nextDouble();
            float accumulatedFitness = 0;

            for (int j = 0; j < rouletteWheel.size(); j++)
            {
                accumulatedFitness += rouletteWheel.get(j);
                if (accumulatedFitness >= randomValue) {
                    parents[i] = population[j];
                    break;
                }
            }
        }

        return parents;
    }

    private static List<Float> proportionalSelection(List<Float> fitnessValues) {

        float percentageSum = 0;
        float min = fitnessValues.get(0);
        float max = fitnessValues.get(0);
        for (float value : fitnessValues) {
            if (value < min) min = value;
            if (value > max) max = value;
        }

        //the percentage of each fitness
        List<Float> relativeFitness = new ArrayList<>();
        for (float value : fitnessValues) {
            float flippedPercentage = (max - value) / (max - min);
            percentageSum += flippedPercentage;
            relativeFitness.add(flippedPercentage);
        }

        //normalize
        for (int i = 0; i < relativeFitness.size(); i++) {
            relativeFitness.set(i, relativeFitness.get(i) / percentageSum);
        }

        return relativeFitness;
    }

    private static Coord3d[] generatePopulation(float xMin, float xMax, float yMin, float yMax) {
        float x;
        float y;
        float z;

        Coord3d[] points = new Coord3d[POPULATION_SIZE];

        Random r = new Random();

        for (int i = 0; i < POPULATION_SIZE; i++) {
            x = xMin + r.nextFloat() * (xMax - xMin);
            y = yMin + r.nextFloat() * (yMax - yMin);
            function.getFunction().setArgumentValue(0, x);
            function.getFunction().setArgumentValue(1, y);
            z = (float)function.getFunction().calculate();
            points[i] = new Coord3d(x, y, z);
        }

        return points;
    }

    private static float findAverage(Coord3d[] population) {
        float sum = 0;
        for (Coord3d coord3d : population) {
            sum += coord3d.z;
        }
        return population.length / sum;
    }

    private static float findMin(Coord3d[] population) {
        float min = population[0].z;
        for (Coord3d coord3d : population) {
            if (coord3d.z < min) min = coord3d.z;
        }
        return min;
    }

}