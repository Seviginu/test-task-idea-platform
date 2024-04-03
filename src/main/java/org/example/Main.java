package org.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

enum FlightStatus {
    SCHEDULED("по расписанию"),
    CANCELED("отменен");


    private final String message;

    FlightStatus(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return this.message;
    }
}

class Flight {
    String no;
    int departure;
    String from;
    String to;
    int duration;
}

class Forecast {
    private final int MAX_WIND = 30;
    private final int MIN_VISIBILITY = 200;

    Map<Integer, FlightStatus> isAvailable = new HashMap<>();

    public void addForecast(int time, int wind, int visibility) {
        isAvailable.put(time, wind <= MAX_WIND && visibility >= MIN_VISIBILITY
                ? FlightStatus.SCHEDULED
                : FlightStatus.CANCELED);
    }

    public FlightStatus getStatus(int time) {
        return isAvailable.get(time);
    }
}

class FlightsAndForecast {
    List<Flight> flights;
    Map<String, Forecast> forecast;
}

class ForecastAdapter extends TypeAdapter<Forecast> {
    @Override
    public void write(JsonWriter jsonWriter, Forecast forecast) throws IOException {

    }

    @Override
    public Forecast read(JsonReader jsonReader) throws IOException {
        Forecast forecast = new Forecast();
        jsonReader.beginArray();
        while (jsonReader.hasNext()) {
            jsonReader.beginObject();
            int time = -1, wind = -1, visibility = -1;
            while (jsonReader.hasNext()) {
                String name = jsonReader.nextName();
                switch (name) {
                    case "time" -> time = jsonReader.nextInt();
                    case "wind" -> wind = jsonReader.nextInt();
                    case "visibility" -> visibility = jsonReader.nextInt();
                }
            }
            if (time == -1 || wind == -1 || visibility == -1)
                throw new IOException();
            forecast.addForecast(time, wind, visibility);
            jsonReader.endObject();
        }
        jsonReader.endArray();
        return forecast;
    }
}

public class Main {
    static final String FILENAME = "flights_and_forecast.json";

    private static void printAll(FlightsAndForecast instance) {
        for (var flight : instance.flights) {
            int depTime = flight.departure;
            int arriveTime = flight.departure + flight.duration;
            Forecast forecastDep = instance.forecast.get(flight.from);
            Forecast forecastArrive = instance.forecast.get(flight.to);
            boolean canFly = forecastDep.getStatus(depTime) == FlightStatus.SCHEDULED &&
                    forecastArrive.getStatus(arriveTime) == FlightStatus.SCHEDULED;
            String message = canFly ? FlightStatus.SCHEDULED.toString() : FlightStatus.CANCELED.toString();
            System.out.printf("%s | %s -> %s | %s\n",
                    flight.no, flight.from, flight.to, message);
        }
    }

    public static void main(String[] args) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(FILENAME)))) {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Forecast.class, new ForecastAdapter())
                    .create();
            FlightsAndForecast instance = gson.fromJson(reader, FlightsAndForecast.class);
            printAll(instance);
        } catch (FileNotFoundException e) {
            System.out.println("Файл не найден");
        } catch (IOException e) {
            System.out.println("Ошибка чтения");
        }

    }
}