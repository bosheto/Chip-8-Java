import static com.raylib.Colors.*;
import static com.raylib.Raylib.*;

public class Main {

    static Memory mem = new Memory();

    public static void main(String args[]) {
        mem.init("test_opcode.ch8");
//        mem.init("IBM Logo.ch8");

        InitWindow(640, 320, "CHIP-8");
        SetTargetFPS(60);
        while (!WindowShouldClose()) {
            mem.run();
            BeginDrawing();
            ClearBackground(RED);
            int[][] screen = mem.getVRAM();
            for(int y = 0; y < mem.getScreen_h(); y ++){
                for(int x = 0; x < mem.getScreen_w(); x++){
                    int color = screen[y][x];
                    int pos_x = x * 10;
                    int pos_y = y * 10;
                    DrawRectangle(pos_x, pos_y, 10, 10, color == 0 ? BLACK : WHITE);
                }
            }
//            DrawRectangle();
            EndDrawing();
        }
        CloseWindow();
    }
}