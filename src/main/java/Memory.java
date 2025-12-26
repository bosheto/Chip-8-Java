import java.io.*;

public class Memory {

    // GPU
    int screen_w = 64;
    int screen_h = 32;
    int[][] VRAM = new int[screen_h][screen_w];

    // CPU
    int[] RAM = new int[4096];
    int PC = 512;
    int I = 0; // Index register, used to point into memory
    int[] V = new int[16]; // Registers
    int[] stack = new int[16];
    int sp = 0;

    int[] Font = {
            0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
            0x20, 0x60, 0x20, 0x20, 0x70, // 1
            0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
            0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
            0x90, 0x90, 0xF0, 0x10, 0x10, // 4
            0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
            0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
            0xF0, 0x10, 0x20, 0x40, 0x40, // 7
            0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
            0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
            0xF0, 0x90, 0xF0, 0x90, 0x90, // A
            0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
            0xF0, 0x80, 0x80, 0x80, 0xF0, // C
            0xE0, 0x90, 0x90, 0x90, 0xE0, // D
            0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
            0xF0, 0x80, 0xF0, 0x80, 0x80  // F
    };

    public Memory() {
        for(int y = 0; y < screen_h; y++){
            for(int x = 0; x < screen_w; x++){
                VRAM[y][x] = 1;
            }
        }
    }

    void LoadFont(){
        for(int i = 0; i < 64; i++){
            RAM[i] = Font[i];
        }
    }

    void LoadFile(String file_name){
        try {
            InputStream input = new FileInputStream(file_name);

            int data;
            int i =0;
            while((data = input.read()) != -1){
                RAM[0x200+i] = data;
                i++;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void init(String file_name){
        for(int i = 0; i < RAM.length; i++){
            RAM[0] = 0;
        }
        LoadFont();
        LoadFile(file_name);
    }

    public void run(){
        decode(fetch());
    }

    String fetch(){
        String opcode = "";
        String f_byte = Integer.toHexString(RAM[PC]);
        String s_byte = Integer.toHexString(RAM[PC+1]);

        f_byte = String.format("%2s", f_byte).replace(' ', '0');
        s_byte = String.format("%2s", s_byte).replace(' ', '0');

        opcode = f_byte + s_byte;
        PC += 2;
        return opcode.toUpperCase();
    }

    void decode(String opcode){
        int op = Integer.parseInt(opcode,16);

        switch (op & 0xF000){
            case 0x0000:
                if (op == 0x00E0) {clear_vram();}
                else if(op == 0x00EE) { // Return from subroutine
                    sp--;
                    PC = stack[sp];
                };
                break;
            case 0xA000: // ANNN set I to NNN
                I = op & 0x0FFF;
                break;
            case 0x6000: // 6XNN Set VX to NN
                int N = (op & 0x0F00) >> 8;
                V[N] = op & 0x00FF;
                break;
            case 0x7000: // #FIXME
                V[(op & 0x0F00) >> 8] += (op & 0x00FF);
                break;
            case 0xD000: // DXYN draw to display
                int X = V[((op & 0x0F00) >> 8) & 63];
                int Y = V[((op & 0x00F0) >> 4) & 31];
                V[0xF] = 0;
                int height = op & 0x000F;
                for(int yLine = 0; yLine < height; yLine++) {
                    int pixel = RAM[I + yLine];
                    for(int xLine = 0; xLine < 8; xLine++){
                        if((pixel & (0x80 >> xLine)) != 0){
                            int xCoord = X+xLine;
                            int yCoord = Y+yLine;
                            if(VRAM[yCoord][xCoord] == 1){
                                V[0xF] = 1;
                            }
                            VRAM[yCoord][xCoord] ^= 1;
                        }
                    }
                }
                break;
            case 0X1000:
                PC = op & 0x0FFF;
                break;
            case 0x3000: // 3XNN increment PC if V[X] = NN
                if(V[(op & 0x0F00) >> 8] == (op & 0x00FF)){
                    PC += 2;
                }
                break;
            case 0x4000:
                if(V[(op & 0x0F00) >> 8] != (op & 0x00FF)){
                    PC += 2;
                }
                break;
            case 0x5000:
                if(V[(op & 0x0F00) >> 8] == V[(op & 0x00F0) >> 4]){
                    PC += 2;
                }
                break;
            case 0x9000: // #FIXME
                if(V[(op & 0x0F00) >> 8] != V[(op & 0x00F0) >> 4]){
                    PC += 2;
                }
                break;
            case 0x2000:
                stack[sp] = PC;
                sp++;
                PC = op & 0x0FFF;
                break;
            case 0x8000:
                decode_la(op);
                break;
            default:
                System.out.printf("Unknown instruction %s !!\n", Integer.toHexString(op));
                break;
        }
    }

    void clear_vram(){
        for(int y = 0; y < screen_h; y++){
            for(int x = 0; x < screen_w; x++){
                VRAM[y][x] = 0;
            }
        }
    }

    void decode_la(int opcode){
        switch (opcode & 0x000F){
            case 0x0000: // 8XY0
                V[(opcode & 0x0F00) >> 8] = V[(opcode & 0x00F0) >> 4];
                break;
            case 0x0001:
                V[(opcode & 0x0F00) >> 8] =  V[(opcode & 0x0F00) >> 8] | V[(opcode & 0x00F0) >> 4];
                break;
            case 0x0002:
                V[(opcode & 0x0F00) >> 8] =  V[(opcode & 0x0F00) >> 8] & V[(opcode & 0x00F0) >> 4];
                break;
            case 0x0003:
                V[(opcode & 0x0F00) >> 8] =  V[(opcode & 0x0F00) >> 8] ^ V[(opcode & 0x00F0) >> 4];
                break;
            case 0x0004:
                int N = V[(opcode & 0x0F00) >> 8] + V[(opcode & 0x00F0) >> 4];
                if(N > 255){
                    V[0xF] = 1;
                }else{
                    V[0xF] = 0;
                }
                V[(opcode & 0x0F00) >> 8] = N & 0xFF;
                break;
            case 0x0005:
                if (V[(opcode & 0x0F00) >> 8] > V[(opcode & 0x00F0) >> 4]){
                    V[0xF] = 1;
                }else{
                    V[0xF] = 0;
                }
                V[(opcode & 0x0F00) >> 8] = (V[(opcode & 0x0F00) >> 8] - V[(opcode & 0x00F0) >> 4]) & 0xFF;
                break;
            case 0x0007:
                if (V[(opcode & 0x00F0) >> 4] > V[(opcode & 0x0F00) >> 8]){
                    V[0xF] = 1;
                }else{
                    V[0xF] = 0;
                }
                V[(opcode & 0x0F00) >> 8] = (V[(opcode & 0x00F0) >> 4] - V[(opcode & 0x0F00) >> 8]) & 0xFF;
                break;
            case 0x0006:
                V[(opcode & 0x0F00) >> 8] = V[(opcode & 0x00F0) >> 4];

            default:
                System.out.printf("Unknown instruction %s !!\n", Integer.toHexString(opcode).toUpperCase());
                break;
        }

    }

    public int[][] getVRAM() {
        return VRAM;
    }

    public int getScreen_w() {
        return screen_w;
    }

    public int getScreen_h() {
        return screen_h;
    }
}
