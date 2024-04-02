package org.example;

import org.jzy3d.colors.Color;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.maths.Range;
import org.jzy3d.maths.Scale;
import org.jzy3d.plot3d.primitives.Scatter;

import java.util.Random;

public class Swarm {

    private static final int SWARM_SIZE = 100;
    private static final int ITERATIONS = 40;
    private static final double INERTIA_FACTOR = 0.5; //usually between 0.4 and 0.9
    private static final double COGNITIVE_COEFFICIENT = 1.6; //usually between 1.5 and 2
    private static final double SOCIAL_COEFFICIENT = 1.6;
    private static final int SLEEP_VALUE = 200;
    private static FunctionParser function;
    private static Plotter plot;
    private static float globalBestFitness;
    private static final Coord3d globalBestPosition = new Coord3d();


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

            Particle[] finalSwarm = run(xRange, yRange);

            System.out.println("Average z value: " + findAverage(finalSwarm));
            System.out.println("Minimum z value: " + findMin(finalSwarm));
            System.out.println("Maximum fitness: " + findBestFitness(finalSwarm).position + " " + findBestFitness(finalSwarm).position.z);

            plot.scatter.setColor(Color.RED);
            plot.chart.render();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Particle[] run(Range xRange, Range yRange) throws InterruptedException {
        Particle[] swarm = generateParticles(xRange.getMin(), xRange.getMax(), yRange.getMin(), yRange.getMax());

        Coord3d[] scatterValues = new Coord3d[SWARM_SIZE];
        for (int i = 0; i < SWARM_SIZE; i++)
        {
            scatterValues[i] = new Coord3d(swarm[i].position.x, swarm[i].position.y, swarm[i].position.z);
        }
        plot.scatter = new Scatter(scatterValues, Color.BLUE);
        plot.scatter.setWidth(30);
        plot.chart.add(plot.scatter);

        globalBestFitness = swarm[0].position.z;

        for (int i = 0; i < ITERATIONS; i++)
        {
            //calculate fitness values and update "best" values if necessary
            for (Particle particle : swarm)
            {
                if (particle.position.z < particle.bestFitness)
                {
                    particle.bestPosition = particle.position;//best position = the one with the best fitness
                    particle.bestFitness = particle.position.z;
                }
            }

            //find the best particle, and update
            findBest(swarm);

            //update parameters
            for (int j = 0; j < SWARM_SIZE; j++)
            {
                swarm[j] = updateParameters(swarm[j]);
            }

            //update the animation
            updateAnimation(swarm, xRange, yRange, scatterValues);
        }

        System.out.println("Best particle position: " + globalBestPosition);
        System.out.println("and its fitness: " + globalBestFitness);
        return swarm;
    }

    public static Particle updateParameters(Particle particle) {

        Random random = new Random();
        double r1 = random.nextDouble();
        double r2 = random.nextDouble();

        //update velocity
        particle.velocity.x = (float) (INERTIA_FACTOR * particle.velocity.x + COGNITIVE_COEFFICIENT * r1 *
                (particle.bestPosition.x - particle.position.x) + SOCIAL_COEFFICIENT * r2 *
                (globalBestPosition.x - particle.position.x));
        r1 = random.nextDouble();
        r2 = random.nextDouble();
        particle.velocity.y = (float) (INERTIA_FACTOR * particle.velocity.y + COGNITIVE_COEFFICIENT * r1 *
                (particle.bestPosition.y - particle.position.y) + SOCIAL_COEFFICIENT * r2 *
                (globalBestPosition.y - particle.position.y));

        //update position
        particle.position.x += particle.velocity.x;
        particle.position.y += particle.velocity.y;
        particle.position.z = fitnessFunction(particle);

        Particle newParticle = new Particle(particle.position, particle.bestPosition, particle.velocity);
        newParticle.bestFitness = particle.bestFitness;

        return newParticle;
    }

    public static void findBest(Particle[] swarm) {

        for (Particle particle : swarm)
        {
            if (particle.position.z < globalBestFitness)
            {
                globalBestFitness = particle.position.z;
                globalBestPosition.x = particle.position.x;
                globalBestPosition.y = particle.position.y;
                globalBestPosition.z = particle.position.z;
            }
        }
    }

    private static float fitnessFunction(Particle particle) {
        function.getFunction().setArgumentValue(0, particle.position.x);
        function.getFunction().setArgumentValue(1, particle.position.y);
        return (float) function.getFunction().calculate();
    }

    private static Particle[] generateParticles(float xMin, float xMax, float yMin, float yMax) {

        Particle[] particles = new Particle[SWARM_SIZE];

        for (int i = 0; i < SWARM_SIZE; i++) {

            Coord3d newPoint = generatePoint(xMin, xMax, yMin, yMax);

            particles[i] = new Particle(newPoint, newPoint, new Coord3d(0, 0, 0));
        }

        return particles;
    }

    private static Coord3d generatePoint(float xMin, float xMax, float yMin, float yMax) {

        Random r = new Random();

        float x = xMin + r.nextFloat() * (xMax - xMin);
        float y = yMin + r.nextFloat() * (yMax - yMin);
        function.getFunction().setArgumentValue(0, x);
        function.getFunction().setArgumentValue(1, y);
        float z = (float)function.getFunction().calculate();

        return new Coord3d(x, y, z);
    }

    public static void updateAnimation(Particle[] swarm, Range xRange, Range yRange, Coord3d[] scatterValues) throws InterruptedException {
        for (int j = 0; j < SWARM_SIZE; j++)
        {
            if (swarm[j].position.x > xRange.getMax() || swarm[j].position.y > yRange.getMax())
            {
                function.getFunction().setArgumentValue(0, xRange.getMax());
                function.getFunction().setArgumentValue(1, yRange.getMax());
                scatterValues[j] = new Coord3d(xRange.getMax(), yRange.getMax(), function.getFunction().calculate());
            }
            else if (swarm[j].position.x < xRange.getMin() || swarm[j].position.y < yRange.getMin())
            {
                function.getFunction().setArgumentValue(0, xRange.getMin());
                function.getFunction().setArgumentValue(1, yRange.getMin());
                scatterValues[j] = new Coord3d(xRange.getMin(), yRange.getMin(), function.getFunction().calculate());
            }
            else scatterValues[j] = swarm[j].position;
        }
        plot.scatter.setData(scatterValues);
        plot.chart.render();
        Thread.sleep(SLEEP_VALUE);
    }

    private static float findAverage(Particle[] particles) {
        float sum = 0;
        for (Particle particle : particles) {
            sum += particle.position.z;
        }
        return particles.length / sum;
    }

    private static Coord3d findMin(Particle[] particles) {
        Coord3d min = particles[0].position;
        for (Particle particle : particles) {
            if (particle.position.z < min.z) min = particle.position;
        }
        return min;
    }

    private static Particle findBestFitness(Particle[] particles) {
        Particle max = particles[0];
        for (Particle particle : particles) {
            if (particle.position.z < max.position.z) max = particle;
        }
        return max;
    }

    public static class Particle {
        public Coord3d position;
        public Coord3d bestPosition;
        public Coord3d velocity;
        public float bestFitness;

        public Particle(Coord3d position, Coord3d bestPosition, Coord3d velocity) {
            this.position = position;
            this.bestPosition = bestPosition;
            this.velocity = velocity;
        }
    }
}


