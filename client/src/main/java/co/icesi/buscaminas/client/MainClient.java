package co.icesi.buscaminas.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import co.icesi.buscaminas.controllers.dtos.Request;
import co.icesi.buscaminas.controllers.dtos.Response;
import co.icesi.buscaminas.model.Cell;

public class MainClient {

    private String host;
    private int port;
    private Gson gson;

    public MainClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.gson = new GsonBuilder().create();
    }

    // Método central para enviar peticiones TCP (Conexión corta por mensaje)
    public Response sendRequest(Request request) {
        try (Socket socket = new Socket(host, port);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // Serializar y enviar con salto de línea (\n)
            String jsonOut = gson.toJson(request);
            writer.write(jsonOut);
            writer.newLine();
            writer.flush();

            // Leer respuesta delimitada por fin de línea
            String jsonIn = reader.readLine();
            return gson.fromJson(jsonIn, Response.class);

        } catch (Exception e) {
            System.err.println("Error de comunicación: " + e.getMessage());
            return null;
        }
    }

    // Renderizado del tablero con colores ANSI
    public void printBoard(Cell[][] board) {
        if (board == null) return;
        System.out.println();
        System.out.print("   ");
        for (int j = 0; j < board[0].length; j++) {
            System.out.print(" " + j);
        }
        System.out.println();
        for (int i = 0; i < board.length; i++) {
            System.out.print(i + " [");
            for (int j = 0; j < board[0].length; j++) {
                System.out.print(" " + board[i][j]);
            }
            System.out.println(" ]");
        }
    }

    public void start() {
        Scanner scanner = new Scanner(System.in);
        boolean exit = false;

        while (!exit) {
            System.out.println("\n=== BUSCAMINAS DISTRIBUIDO - CLIENTE TCP ===");
            System.out.println("[1] Iniciar nueva partida");
            System.out.println("[2] Destapar celda");
            System.out.println("[3] Marcar / Desmarcar bandera");
            System.out.println("[4] Consultar estado actual del tablero");
            System.out.println("[5] Rendirse y revelar tablero completo");
            System.out.println("[6] Salir");
            System.out.print("Seleccione una opción: ");

            int option = scanner.nextInt();
            Request req = new Request();
            req.data = new HashMap<>();

            switch (option) {
                case 1:
                    System.out.print("Filas (n): ");
                    req.data.put("n", String.valueOf(scanner.nextInt()));
                    System.out.print("Columnas (m): ");
                    req.data.put("m", String.valueOf(scanner.nextInt()));
                    System.out.print("Minas: ");
                    req.data.put("minas", String.valueOf(scanner.nextInt()));
                    req.action = "INIT_GAME";
                    break;

                case 2:
                    System.out.print("Fila (i): ");
                    req.data.put("i", String.valueOf(scanner.nextInt()));
                    System.out.print("Columna (j): ");
                    req.data.put("j", String.valueOf(scanner.nextInt()));
                    req.action = "SELECT_CELL";
                    break;

                case 3:
                    System.out.print("Fila (i): ");
                    req.data.put("i", String.valueOf(scanner.nextInt()));
                    System.out.print("Columna (j): ");
                    req.data.put("j", String.valueOf(scanner.nextInt()));
                    req.action = "MARK_CELL";
                    break;

                case 4:
                    req.action = "GET_BOARD";
                    break;

                case 5:
                    req.action = "SOW_ALL";
                    break;

                case 6:
                    exit = true;
                    continue;

                default:
                    System.out.println("Opción no válida.");
                    continue;
            }

            Response res = sendRequest(req);
            if (res != null && res.data != null) {
                // Convertir la clave "board" del Map a una matriz Cell[][]
                if (res.data.containsKey("board")) {
                    String boardJson = gson.toJson(res.data.get("board"));
                    Cell[][] board = gson.fromJson(boardJson, Cell[][].class);
                    printBoard(board);
                }

                // Detección de fin de partida
                Boolean gameEnd = (Boolean) res.data.get("gameEnd");
                Boolean win = (Boolean) res.data.get("win");

                if (Boolean.TRUE.equals(gameEnd)) {
                    if (Boolean.TRUE.equals(win)) {
                        System.out.println("\n¡FELICIDADES! Has ganado la partida.");
                    } else {
                        System.out.println("\n¡BOOM! Tocaste una mina. Fin del juego.");
                    }
                }
            }
        }
        scanner.close();
    }

    public static void main(String[] args) {
        MainClient client = new MainClient("localhost", 12345);
        client.start();
    }
}