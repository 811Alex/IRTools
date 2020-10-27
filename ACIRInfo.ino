/*
  Tool to get info about an A/C IR remote signal.
  You will need a TSOP to receive the signal.
  You can change the settings bellow, but the tool should read a lot of signals out of the box.
  Repository: https://github.com/811Alex/IRTools
*/
// Settings
const short TSOP = 2;               // IR sensor pin
const short LED = 13;               // status indicator led pin
const short MODE = 0;             // 0: NORMAL, 1: count pulses & bytes, 2: print length of separators
const bool SKIP_LENGTHS = true;    // for normal mode, skips printing the pulse lengths
// Decoding settings
const short BITS = 8;
const short BYTES = 7;              // bytes per signal
const int START_BIT_LENGTH = 1000;  // length of first bit to register a signal (microseconds)

bool data[BYTES][BITS];   // binary data
bool prev[BYTES][BITS];   // previous bin data
int raw[BYTES][BITS];     // raw pulse lengths
int start_length;         // last start bit length
int count = 0;            // signal count
int binThreshold = 0;     // anything longer than this will be considered a 1
bool ledState = false;

void setup(){
  pinMode(TSOP,INPUT);
  pinMode(LED,OUTPUT);
  Serial.begin(9600);
  Serial.println("Ready!\n");
}

void loop(){
  digitalWrite(LED, ledState=!ledState);  // blink led
  switch(MODE){
    case 1: pulseCount(); break;
    case 2: readSeparators(); break;
    default:
      if((start_length = pulseIn(TSOP,LOW)) > START_BIT_LENGTH){ // Check if the Start Bit has been received.
        //pulseIn(TSOP,HIGH);
        readSignal();   // read from TSOP
        decodeSignal(); // decode and print info
        delay(150);     // wait before reading next
      }
  }
}

void decodeSignal(){
  if(binThreshold == 0) calibrate();  // find appropriate threshold for binary decoding
  Serial.print("Signal:\t\t");
  Serial.println(++count);
  Serial.print("Start bit:\t");
  Serial.print(start_length);
  if(!SKIP_LENGTHS){
    Serial.print("μs\nLengths:\t");
    for(int i = 0; i<BYTES; i++)
      for(int j = 0; j<BITS; j++){  // print raw data
        Serial.print(raw[i][j]);
        Serial.print(' ');
      }
  }
  else Serial.print("μs");
  Serial.print("\nData:\t\t");
  for(int i = 0; i<BYTES; i++){
    for(int j = 0; j<BITS; j++){  // decode and print decoded data
      prev[i][j] = data[i][j];
      data[i][j] = (raw[i][j] > binThreshold);
      Serial.print(data[i][j]);
    }
    Serial.print(' ');
  }
  if(count>1){
    Serial.print("\nDiff:\t\t");
    for(int i = 0; i<BYTES; i++){
      for(int j = 0; j<BITS; j++) // show differences from last signal
        Serial.print((data[i][j] == prev[i][j]) ? ' ' : '^');
      Serial.print(' ');
    }
  }
  Serial.println('\n');
}

void pulseCount(){
  int i = 0;
  while(pulseIn(TSOP,HIGH,100000)>0) i++;  // count pulses
  if(i>0){
    Serial.print("Pulses: ");
    Serial.println(i);
    Serial.print("Bytes: ");
    Serial.print((float)(i-1)/BITS);      // exclude start bit
    Serial.println(" (+ start bit)\n");
  }
}

void readSeparators(){  // print average separator length
  int length, avg;
  if((avg = pulseIn(TSOP,LOW,100000))>0){
    while((length = pulseIn(TSOP,LOW,100000)) > 0)
      avg = (avg+length)/2;
    Serial.print("Avg. separator length:\t");
    Serial.print(avg);
    Serial.println("μs");
  }
}

void readSignal(){
  for(int i = 0; i<BYTES; i++)
    for(int j = 0; j<BITS; j++)
      raw[i][j] = pulseIn(TSOP,HIGH,100000);  // save pulse train
}

void calibrate(){ // find appropriate threshold, for binary decoding, using K-means on the data (excluding the start bit)
  Serial.println("Calibrating...");
  int bin0 = 0, bin1 = 0;   // pulse lengths for 1 & 0
  int flatRaw[BYTES*BITS];
  flattenArray(raw, flatRaw);
  int centers[] = {flatRaw[0], flatRaw[1]}; // init centers
  kMeansC(centers, flatRaw, BITS*BYTES);    // run K-means to get the centers of the two clusters
  bin0 = centers[0];
  bin1 = centers[1];
  if(bin1<bin0){        // the longer pulse is a 1
    bin0 = centers[1];
    bin1 = centers[0];
  }
  binThreshold = (bin0+bin1)/2;     // use the center average as the threshold
  Serial.print("Avg. 0 pulse:\t");
  Serial.print(bin0);
  Serial.print("μs\nAvg. 1 pulse:\t");
  Serial.print(bin1);
  Serial.print("μs\nThreshold:\t");
  Serial.print(binThreshold);
  Serial.println("μs\nCalibrated!\n");
}

void flattenArray(int in[][BITS], int out[]){ // 2d -> 1d signal array
  for(int i=0; i<BYTES; i++)
    for(int j=0; j<BITS; j++)
      out[BITS*i+j] = in[i][j];
}

void kMeansC(int centers[], int set[], int setSize){ // Apply K-means on a set (2 clusters) to get the centers
  int newCenters[2];
  int subsets[2][setSize];
  int i = 0, j = 0;
  long sum = 0;
  // split to two clusters
  for(int n=0; n<setSize; n++)
    if(absolute(centers[0]-set[n])<absolute(centers[1]-set[n])) subsets[0][i++]=set[n];
    else subsets[1][j++]=set[n];
  // find new centers
  for(int k=0; k<i; k++) sum+=subsets[0][k];
  newCenters[0]=(i>0) ? sum/i : 0;
  sum=0;
  for(int k=0; k<j; k++) sum+=subsets[1][k];
  newCenters[1]=(j>0) ? sum/j : 0;
  // recurse until the centers don't change
  if(centers[0]!=newCenters[0] || centers[1]!=newCenters[1]){
    centers[0]=newCenters[0];
    centers[1]=newCenters[1];
    kMeansC(centers,set,setSize);
  }
}

int absolute(int n){
  return (n<0)?-n:n;
}
