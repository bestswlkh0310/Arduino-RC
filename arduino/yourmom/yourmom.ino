#include <SoftwareSerial.h>
#define BT_RXD 8
#define BT_TXD 7

#define DOWN_R 1
#define DOWN_F 2
#define DOWN_B 3
#define UP_L 4
#define UP_R 5
#define UP_F 6
#define UP_B 7

#define SPEED1 6
#define IN1 5
#define IN2 4

#define SPEED2 3
#define IN3 2
#define IN4 9

#define SPEED3 12
#define IN5 11
#define IN6 10

bool isFront = false;

SoftwareSerial bluetooth(BT_RXD, BT_TXD);
void setup() {
  Serial.begin (9600);
  bluetooth.begin(9600);

  pinMode(SPEED1, OUTPUT);
  pinMode(IN1, OUTPUT);
  pinMode(IN2, OUTPUT);

  pinMode(SPEED2, OUTPUT);
  pinMode(IN3, OUTPUT);
  pinMode(IN4, OUTPUT);

  pinMode(SPEED3, OUTPUT);
  pinMode(IN5, OUTPUT);
  pinMode(IN6, OUTPUT);
}
void loop() {
  bt();
  move();
}

void move() {
  if (isFront == true) {
    digitalWrite(IN1, HIGH);
    digitalWrite(IN2, LOW);
    analogWrite(SPEED1, 255);

    digitalWrite(IN3, HIGH);
    digitalWrite(IN4, LOW);
    analogWrite(SPEED2, 255);

    digitalWrite(IN5, HIGH);
    digitalWrite(IN6, LOW);
    analogWrite(SPEED3, 255);
  } else {
    Serial.println("false!");
    digitalWrite(IN1, LOW);
    digitalWrite(IN2, LOW);
    analogWrite(SPEED1, 0); 

    digitalWrite(IN3, LOW);
    digitalWrite(IN4, LOW);
    analogWrite(SPEED2, 0);

    digitalWrite(IN5, LOW);
    digitalWrite(IN6, LOW);
    analogWrite(SPEED3, 0);

  }
}

void bt() {
  if (bluetooth.available()) {
    char i = bluetooth.read();
    Serial.write(i);
    handleInput(i);    
  }
  if (Serial.available()) {
    bluetooth.write(Serial.read());
  }
}

void handleInput(char i) {
  if (i == '0') {
    isFront = true;
  } else if (i == '1') {
    isFront = false;
  }
}

