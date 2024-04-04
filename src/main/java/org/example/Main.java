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

class TimeZoneConverter {
    Map<String, Integer> cityToUTC = new HashMap<>();

    public int getDifference(String city1, String city2) {
        return cityToUTC.getOrDefault(city1, 0) -
                cityToUTC.getOrDefault(city2, 0);
    }

    public void addCity(String city, int timeZoneUTC) {
        cityToUTC.put(city, timeZoneUTC);
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

    private static void printAll(FlightsAndForecast instance, TimeZoneConverter converter) {
        for (var flight : instance.flights) {
            int depTime = flight.departure;
            int offset = converter.getDifference(flight.to, flight.from);
            int arriveTime = flight.departure + flight.duration + offset;
            Forecast forecastDep = instance.forecast.get(flight.from);
            Forecast forecastArrive = instance.forecast.get(flight.to);

            boolean canFly = forecastDep.getStatus(depTime) == FlightStatus.SCHEDULED &&
                    forecastArrive.getStatus(arriveTime) == FlightStatus.SCHEDULED;
            String message = canFly ? FlightStatus.SCHEDULED.toString() : FlightStatus.CANCELED.toString();
            System.out.printf("%s | %s -> %s | %s\n",
                    flight.no, flight.from, flight.to, message);
        }
    }

    private static void registerAllCities(TimeZoneConverter converter) {
        converter.addCity("moscow", 3);
        converter.addCity("novosibirsk", 7);
        converter.addCity("omsk", 6);
    }

    public static void main(String[] args) {
        TimeZoneConverter converter = new TimeZoneConverter();
        registerAllCities(converter);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(FILENAME)))) {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Forecast.class, new ForecastAdapter())
                    .create();
            FlightsAndForecast instance = gson.fromJson(reader, FlightsAndForecast.class);
            printAll(instance, converter);
        } catch (FileNotFoundException e) {
            System.out.println("Файл не найден");
        } catch (IOException e) {
            System.out.println("Ошибка чтения");
        }

    }
}