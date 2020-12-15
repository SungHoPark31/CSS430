#include <sys/types.h>   // for fork, wait
#include <sys/wait.h>    // for wait
#include <unistd.h>      // for fork, pipe, dup, close
#include <stdio.h>       // for NULL, perror
#include <stdlib.h>      // for exit

#include <iostream>      // for cout

using namespace std;

int main( int argc, char** argv ) {
  //Create array for pipe later
  int fds[2][2];

  //Initialize process identifier
  int pid;

  //If the main function fails
  if ( argc != 2 ) {
    cerr << "Usage: processes command" << endl;
    exit( -1 );
  }

  // Fork a Child
  if ((pid = fork()) < 0 ) {
    //If the fork is a negative number, then fail.
    perror( "fork error" );
    exit(-1);
  }
  //If fork is 0, then child is created. You are child
  else if(pid == 0)  {

    // Create a pipe using fds[0]
    if((pipe(fds[0])) < 0) {
      //If you can't create it, error message
      exit(-1);
    }

    //If the pipe is  created, fork a grandchild
    pid = fork();

    if(pid < 0) {
      perror("Fork error");
      exit(-1);
    }
    //if I'm a grand-child
    else if(pid == 0) {

      //Create a pipe using fds[1]
      if((pipe(fds[1])) < 0) {
        exit(-1);
      }
        
      //Fork a great-grand-child
      pid = fork();

      if(pid < 0) {
        perror("Fork error");
        exit(-1);
      }
      else if(pid == 0) {
        //If I'm a great-grand-child, close all unnecessary pipe streams    
        close(fds[0][0]);
	close(fds[0][1]);
      	close(fds[1][0]);          

	//Exchange data through the write end of the pipe 
	//shared with grand child
	dup2(fds[1][1], 1);

	//execute "ps -A" and pipe to grand child
	execlp("ps", "ps", "-A", NULL);
      }
      else {
        //If I am grandchild, read from great-grand child
	//Close all unnecessary pipe streams	   
	close(fds[1][1]);
	close(fds[0][0]);

	//Exchange data through the read end of the pipe 
	//shared with great-grand child.
        dup2(fds[1][0], 0);

	//Exchange data through the write end of the pipe
	//shared with the child 
        dup2(fds[0][1], 1);
        wait(NULL);

	//Execute "grep argv[1]" and pipe to child   
        execlp("grep", "grep", argv[1], NULL);
      }
    }
    else {
      //Else if I'm a child, read from grandchild.
      //Close all unnecessary pipe flow.
      close(fds[0][1]);
      close(fds[1][0]);
      close(fds[1][1]);

      //Exchange data through the read end of the pipe 
      //shared with grand child.
      dup2(fds[0][0], 0);
      wait(NULL);

      //Execute "ws -l" 
      execlp("wc", "wc", "-l", NULL);
    }
  }
  else {
    // I'm a parent
    wait( NULL );
    cout << "commands completed" << endl;
  }
  return 0;
}
