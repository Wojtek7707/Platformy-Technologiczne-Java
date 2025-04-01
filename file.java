//package org.example;
//
//import java.util.*;
//import java.util.concurrent.*;
//import java.util.concurrent.locks.*;
//
//class StudentProcessor {
//    private static volatile boolean isRunning = true;
//    private static int currentId = 1;
//    private static final BlockingQueue<Student> studentQueue = new LinkedBlockingQueue<>();
//    private static final ResultsStorage resultsStorage = new ResultsStorage();
//
//    static class Subject {
//        final String name;
//        final int ects;
//
//        public Subject(String name, int ects) {
//            this.name = name;
//            this.ects = ects;
//        }
//    }
//
//    static class Student {
//        final int id;
//        final List<SubjectResult> results;
//
//        public Student(int id, List<SubjectResult> results) {
//            this.id = id;
//            this.results = Collections.unmodifiableList(results);
//        }
//    }
//
//    static class SubjectResult {
//        final Subject subject;
//        final boolean passed;
//
//        public SubjectResult(Subject subject, boolean passed) {
//            this.subject = subject;
//            this.passed = passed;
//        }
//    }
//
//    static class ResultsStorage {
//        private final Lock lock = new ReentrantLock();
//        private final List<StudentResult> results = new ArrayList<>();
//
//        static class StudentResult {
//            final int studentId;
//            final boolean passed;
//            final long processingTime;
//
//            public StudentResult(int studentId, boolean passed, long processingTime) {
//                this.studentId = studentId;
//                this.passed = passed;
//                this.processingTime = processingTime;
//            }
//        }
//
//        public void addResult(int studentId, boolean passed, long processingTime) {
//            lock.lock();
//            try {
//                results.add(new StudentResult(studentId, passed, processingTime));
//            } finally {
//                lock.unlock();
//            }
//        }
//
//        public void printStatistics() {
//            lock.lock();
//            try {
//                long totalTime = results.stream().mapToLong(r -> r.processingTime).sum();
//                long passedCount = results.stream().filter(r -> r.passed).count();
//
//                System.out.println("\n=== Statystyki ===");
//                System.out.printf("Przetworzonych studentów: %d\n", results.size());
//                System.out.printf("Zaliczyło: %d (%.1f%%)\n", passedCount,
//                        (passedCount * 100.0 / results.size()));
//                System.out.printf("Łączny czas przetwarzania: %d ms\n", totalTime);
//                System.out.printf("Średni czas przetwarzania: %.2f ms\n",
//                        results.stream().mapToLong(r -> r.processingTime).average().orElse(0));
//            } finally {
//                lock.unlock();
//            }
//        }
//    }
//
//    public static void main(String[] args) {
//        final List<Subject> subjects = List.of(
//                new Subject("PAA", 3), new Subject("AKO", 6), new Subject("GK", 4),
//                new Subject("BD", 6), new Subject("JP", 3), new Subject("MII", 4),
//                new Subject("WF", 1), new Subject("FW", 3), new Subject("JA", 2)
//        );
//
//        int totalStudents = 10000;
//        int consumerThreads = Runtime.getRuntime().availableProcessors();
//
//        ExecutorService producerExecutor = Executors.newSingleThreadExecutor();
//        ExecutorService consumerExecutor = Executors.newFixedThreadPool(consumerThreads);
//
//        // Wątek produkcyjny
//        producerExecutor.submit(() -> {
//            try {
//                while (currentId <= totalStudents && isRunning) {
//                    Student student = generateStudent(currentId, subjects);
//                    synchronized (System.out) {
//                        System.out.println("Wygenerowano studenta ID: " + student.id);
//                    }
//                    studentQueue.put(student);
//                    currentId++;
//                }
//                studentQueue.put(new Student(-1, List.of())); // Sygnał zakończenia
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        });
//
//        // Wątki konsumenckie
//        for (int i = 0; i < consumerThreads; i++) {
//            consumerExecutor.submit(() -> {
//                try {
//                    while (isRunning) {
//                        Student student = studentQueue.take();
//                        if (student.id == -1) {
//                            studentQueue.put(student); // Przekaż sygnał dalej
//                            break;
//                        }
//
//                        long startTime = System.currentTimeMillis();
//                        boolean passed = calculatePassStatus(student);
//                        long processingTime = System.currentTimeMillis() - startTime;
//
//                        resultsStorage.addResult(student.id, passed, processingTime);
//                    }
//                } catch (InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                }
//            });
//        }
//
//        // Obsługa komendy exit
//        new Thread(() -> {
//            Scanner scanner = new Scanner(System.in);
//            while (true) {
//                if ("exit".equalsIgnoreCase(scanner.nextLine())) {
//                    isRunning = false;
//                    producerExecutor.shutdownNow();
//                    consumerExecutor.shutdownNow();
//                    break;
//                }
//            }
//            scanner.close();
//        }).start();
//
//        try {
//            producerExecutor.awaitTermination(1, TimeUnit.MINUTES);
//            consumerExecutor.awaitTermination(1, TimeUnit.MINUTES);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }
//
//        resultsStorage.printStatistics();
//    }
//
//    private static Student generateStudent(int id, List<Subject> subjects) {
//        Random rand = new Random();
//        List<SubjectResult> results = new ArrayList<>();
//        int subjectsCount = rand.nextInt(subjects.size()) + 1;
//
//        while (results.size() < subjectsCount) {
//            Subject subject = subjects.get(rand.nextInt(subjects.size()));
//            if (results.stream().noneMatch(r -> r.subject == subject)) {
//                results.add(new SubjectResult(subject, rand.nextBoolean()));
//            }
//        }
//        return new Student(id, results);
//    }
//
//    private static boolean calculatePassStatus(Student student) {
//        int totalEcts = 0;
//        int passedEcts = 0;
//
//        for (SubjectResult result : student.results) {
//            totalEcts += result.subject.ects;
//            if (result.passed) passedEcts += result.subject.ects;
//        }
//
//        return passedEcts >= totalEcts / 2;
//    }
//}
package org.example;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

class StudentProcessorTest {

    // Współdzielone zasoby i flagi
    private static volatile boolean isRunning = true;
    private static int currentId = 1;
    private static final BlockingQueue<Student> studentQueue = new LinkedBlockingQueue<>();
    private static final ResultsStorage resultsStorage = new ResultsStorage();

    // Dane pomocnicze – przedmioty (mogą być również wczytywane lub generowane)
    private static final List<Subject> subjects = List.of(
            new Subject("PAA", 3), new Subject("AKO", 6), new Subject("GK", 4),
            new Subject("BD", 6), new Subject("JP", 3), new Subject("MII", 4),
            new Subject("WF", 1), new Subject("FW", 3), new Subject("JA", 2)
    );

    // Liczba studentów do przetworzenia
    private static final int totalStudents = 10000;

    // Klasa reprezentująca przedmiot
    static class Subject {
        final String name;
        final int ects;

        public Subject(String name, int ects) {
            this.name = name;
            this.ects = ects;
        }
    }

    // Klasa reprezentująca studenta
    static class Student {
        final int id;
        final List<SubjectResult> results;

        public Student(int id, List<SubjectResult> results) {
            this.id = id;
            this.results = Collections.unmodifiableList(results);
        }
    }

    // Klasa reprezentująca wynik z przedmiotu
    static class SubjectResult {
        final Subject subject;
        final boolean passed;

        public SubjectResult(Subject subject, boolean passed) {
            this.subject = subject;
            this.passed = passed;
        }
    }

    // Klasa magazynu wyników z synchronizacją
    static class ResultsStorage {
        private final Lock lock = new ReentrantLock();
        private final List<StudentResult> results = new ArrayList<>();

        static class StudentResult {
            final int studentId;
            final boolean passed;
            final long processingTime;

            public StudentResult(int studentId, boolean passed, long processingTime) {
                this.studentId = studentId;
                this.passed = passed;
                this.processingTime = processingTime;
            }
        }

        public void addResult(int studentId, boolean passed, long processingTime) {
            lock.lock();
            try {
                results.add(new StudentResult(studentId, passed, processingTime));
            } finally {
                lock.unlock();
            }
        }

        // Metoda do czyszczenia wyników przed każdym testem
        public void clear() {
            lock.lock();
            try {
                results.clear();
            } finally {
                lock.unlock();
            }
        }

        public void printStatistics() {
            lock.lock();
            try {
                long totalTime = results.stream().mapToLong(r -> r.processingTime).sum();
                long passedCount = results.stream().filter(r -> r.passed).count();

                System.out.println("\n=== Statystyki ===");
                System.out.printf("Przetworzonych studentów: %d\n", results.size());
                System.out.printf("Zaliczyło: %d (%.1f%%)\n", passedCount,
                        (passedCount * 100.0 / results.size()));
                System.out.printf("Łączny czas przetwarzania: %d ms\n", totalTime);
                System.out.printf("Średni czas przetwarzania: %.2f ms\n",
                        results.stream().mapToLong(r -> r.processingTime).average().orElse(0));
            } finally {
                lock.unlock();
            }
        }
    }

    // Metoda generująca studenta z losowymi wynikami
    private static Student generateStudent(int id, List<Subject> subjects) {
        Random rand = new Random();
        List<SubjectResult> results = new ArrayList<>();
        int subjectsCount = rand.nextInt(subjects.size()) + 1;

        while (results.size() < subjectsCount) {
            Subject subject = subjects.get(rand.nextInt(subjects.size()));
            if (results.stream().noneMatch(r -> r.subject == subject)) {
                results.add(new SubjectResult(subject, rand.nextBoolean()));
            }
        }
        return new Student(id, results);
    }

    // Metoda obliczająca czy student zdał (na podstawie sum ECTS)
    private static boolean calculatePassStatus(Student student) {
        int totalEcts = 0;
        int passedEcts = 0;

        for (SubjectResult result : student.results) {
            totalEcts += result.subject.ects;
            if (result.passed) passedEcts += result.subject.ects;
        }
        return passedEcts >= totalEcts / 2;
    }

    // Metoda wykonująca test dla podanej liczby wątków konsumenckich
    public static long runTest(int consumerThreads) {
        // Reset stanu
        isRunning = true;
        currentId = 1;
        studentQueue.clear();
        resultsStorage.clear();

        // Utworzenie executorów
        ExecutorService producerExecutor = Executors.newSingleThreadExecutor();
        ExecutorService consumerExecutor = Executors.newFixedThreadPool(consumerThreads);

        long startTime = System.currentTimeMillis();

        // Wątek produkcyjny – generuje studentów
        producerExecutor.submit(() -> {
            try {
                while (currentId <= totalStudents && isRunning) {
                    Student student = generateStudent(currentId, subjects);
                    studentQueue.put(student);
                    currentId++;
                }
                // Sygnał zakończenia
                studentQueue.put(new Student(-1, List.of()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Wątki konsumenckie – pobierają studentów i przetwarzają ich
        for (int i = 0; i < consumerThreads; i++) {
            consumerExecutor.submit(() -> {
                try {
                    while (isRunning) {
                        Student student = studentQueue.take();
                        if (student.id == -1) {
                            // Przekaż sygnał zakończenia kolejnym wątkom
                            studentQueue.put(student);
                            break;
                        }
                        long procStart = System.currentTimeMillis();
                        boolean passed = calculatePassStatus(student);
                        long processingTime = System.currentTimeMillis() - procStart;
                        resultsStorage.addResult(student.id, passed, processingTime);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Zakończenie pracy executorów
        producerExecutor.shutdown();
        consumerExecutor.shutdown();

        try {
            producerExecutor.awaitTermination(5, TimeUnit.MINUTES);
            consumerExecutor.awaitTermination(5, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }

    // Główna metoda testująca działanie dla wątków od 1 do 14
    public static void main(String[] args) {
        Map<Integer, Long> testResults = new LinkedHashMap<>();

        System.out.println("Test efektywności wielowątkowości (przetwarzanie " + totalStudents + " studentów):");
        for (int threads = 1; threads <= 14; threads++) {
            long time = runTest(threads);
            testResults.put(threads, time);
            System.out.printf("Liczba wątków: %2d | Czas przetwarzania: %5d ms%n", threads, time);
        }

        // Przykładowe wypisanie wyników w formacie CSV (do dalszej analizy/wykresów)
        System.out.println("\nWyniki (CSV):");
        System.out.println("Wątki;Czas_ms");
        testResults.forEach((threads, time) ->
                System.out.println(threads + ";" + time)
        );

        // Możesz wywołać także metodę printStatistics, aby uzyskać dodatkowe statystyki przetwarzania:
        resultsStorage.printStatistics();

        /*
         * DRUK RAPORTU I ANALIZY:
         * 1. W raporcie należy przedstawić wykresy ilustrujące czas przetwarzania (oś Y)
         *    w zależności od liczby wątków (oś X). Możesz wykorzystać dane z CSV i np.
         *    narzędzie Excel, Python matplotlib lub JFreeChart.
         *
         * 2. Opis zakresu użycia wielowątkowości:
         *    - Wielowątkowość realizowana jest przez uruchomienie osobnych wątków konsumenckich,
         *      które pobierają dane z współdzielonej kolejki i wykonują obliczenia.
         *    - Wątek produkcyjny generuje dane i umieszcza je w kolejce.
         *    - Mechanizmy synchronizacji (BlockingQueue, Lock) zapewniają bezpieczeństwo danych.
         *
         * 3. Wnioski:
         *    - Wyniki testów pozwalają określić optymalną liczbę wątków dla danego obciążenia.
         *    - Ograniczenia sprzętowe (liczba rdzeni procesora, pamięć) wpływają na efektywność.
         *    - Przy bardzo małej liczbie wątków czas przetwarzania jest wydłużony, a przy zbyt dużej
         *      liczbie wątków mogą pojawić się koszty synchronizacji, co również wpływa na wydajność.
         *
         * W raporcie warto szczegółowo opisać zarówno wyniki pomiarów, jak i obserwacje dotyczące
         * skalowalności przetwarzania oraz ograniczenia wynikające z architektury sprzętowej.
         */
    }
}
