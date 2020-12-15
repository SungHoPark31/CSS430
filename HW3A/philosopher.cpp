#include <pthread.h> // pthread
#include <stdio.h>
#include <stdlib.h>   // atoi( )
#include <unistd.h>   // sleep( )
#include <sys/time.h> // gettimeofday
#include <iostream>   // cout

#define PHILOSOPHERS 5
#define MEALS 3

using namespace std;

class Table2 {
public:
  Table2( ) {
    // initialize by yourself
    pthread_mutex_init(&lock, NULL);

    //Initialize the philosophers to THINKING 
    for(int i = 0; i < 5; i++) { 
      state[i] = THINKING;  
      pthread_cond_init(&self[i], NULL);
    }
  }
  void pickup( int i ) {
    //Make sure to lock  
    pthread_mutex_lock(&lock);

    //I got Hungry    
    state[i] = HUNGRY;

    //Check if you can have the left and right chopsticks
    test(i);

    //If you can't, then you should wait.
    if(state[i] != EATING)
      pthread_cond_wait(&self[i], &lock);

    cout << "philosopher[" << i << "] picked up chopsticks" << endl;

    //Make sure to unlock.
    pthread_mutex_unlock(&lock);  
  }

  void putdown( int i ) {
    //Make sure to lock
    pthread_mutex_lock(&lock);
    cout << "philosopher[" << i << "] put down chopsticks" << endl;

    //I am full so I am thinking.
    state[i] = THINKING;

    //Check if my left and right need to be woken
    test((i + 4) % 5);
    test((i + 1) % 5);

    //Unlock
    pthread_mutex_unlock(&lock);
  }

private:
  enum { THINKING, HUNGRY, EATING } state[PHILOSOPHERS];
  pthread_mutex_t lock;
  pthread_cond_t self[PHILOSOPHERS];

  void test( int i ) {
    //If left and right is not eating from i, then i can wat.
    if((state[(i + 4) % 5] != EATING) && (state[i] == HUNGRY) 
       && (state[(i + 1) % 5] != EATING)) {
      state[i] = EATING;

      //Wake up i
      pthread_cond_signal(&self[i]);
    }
  }
};

class Table1 {
public:
  Table1( ) {
    // initialize the mutex lock
    pthread_mutex_init(&lock, NULL);
  }
  void pickup( int i ) {
    // lock by yourself
    pthread_mutex_lock(&lock);
    cout << "philosopher[" << i << "] picked up chopsticks" << endl;
  }
 
  void putdown( int i ) {
    cout << "philosopher[" << i << "] put down chopsticks" << endl;
    // unlock by yourself
    pthread_mutex_unlock(&lock);
  }

private:
  // define a mutex lock
  pthread_mutex_t lock;
};

class Table0 {
public:
  void pickup( int i ) {
    cout << "philosopher[" << i << "] picked up chopsticks" << endl;
  }

  void putdown( int i ) {
    cout << "philosopher[" << i << "] put down chopsticks" << endl;
  }
};

static Table2 table2;
static Table1 table1;
static Table0 table0;

static int table_id = 0;

void *philosopher( void *arg ) {
  int id = *(int *)arg;
  
  for ( int i = 0; i < MEALS; i++ ) {
    switch( table_id ) {
    case 0:
      table0.pickup( id );
      sleep( 1 );
      table0.putdown( id );
      break;
    case 1:
      table1.pickup( id );
      sleep( 1 );
      table1.putdown( id );
      break;
    case 2:
      table2.pickup( id );
      sleep( 1 );
      table2.putdown( id );
    break;
    }
  }
  return NULL;
}

int main( int argc, char** argv ) {
  pthread_t threads[PHILOSOPHERS];
  pthread_attr_t attr;
  int id[PHILOSOPHERS];
  table_id = atoi( argv[1] );

  pthread_attr_init(&attr);
  
  struct timeval start_time, end_time;
  gettimeofday( &start_time, NULL );
  for ( int i = 0; i < PHILOSOPHERS; i++ ) {
    id[i] = i;
    pthread_create( &threads[i], &attr, philosopher, (void *)&id[i] );
  }

  for ( int i = 0; i < PHILOSOPHERS; i++ )
    pthread_join( threads[i], NULL );
  gettimeofday( &end_time, NULL );

  sleep( 1 );
  cout << "time = " << ( end_time.tv_sec - start_time.tv_sec ) * 1000000 + ( end_time.tv_usec - start_time.tv_usec ) << endl;

  return 0;
}
