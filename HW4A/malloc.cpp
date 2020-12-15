#include <unistd.h> // sbrk( )
#include <limits.h> // LONG_MAX

using namespace std;

static bool initialized = false;
static void *heap_top; // the beginning of the heap space
static void *heap_end; // the current boundary of the heap space, obtained from sbrk( 0 )

class MCB { // memory control block
public:
  int available; // true(1): this memory partition is available, false(0) unavailalbe.
  int size;      // MCB size + the user data size
};

void free_( void *dealloc_space ) {
  MCB *mcb;

  // locate this partition's mcb address from dealloc_space
  mcb = (MCB *)((unsigned long long int)dealloc_space - sizeof(MCB));
  //Change to available  
  mcb->available = 1;
  return;
}

void *malloc_f( long size ) {
  struct MCB *cur_mcb;          // current MCB
  void *new_space = NULL; // this is a pointer to a new memory space allocated for a user

  if( !initialized )   {
    // find the end of heap memory, upon an initialization
    heap_end = sbrk( 0 );
    heap_top = heap_end;
    initialized = true;
  }

  // append an MCB in front of a requested memroy space
  size = size + sizeof( MCB );

  //Scan each mcb from top to bottom of the heap
  for(void *cur = heap_top; cur < heap_end; cur = (void *)((unsigned long long int)cur + cur_mcb->size)) {
    //Set current mcb to current and make sure to type cast because cur is a void
    cur_mcb = (MCB *)cur;

    //Check if cur_mcb is available and if the size of the mcb is 
    //greater than or equal to the size that needs to be allocated.
    if((cur_mcb->available == 1) && (cur_mcb->size >= size)) {
      //It's filled in now so make it unavailable
      cur_mcb->available = 0;

      //Allocate the memory into this space. 
      new_space = cur_mcb; 

      //break from the loop: This is first fit so once it's found and
      //memory is allocated, then don't scan the heap anymore.
      break;
    }
  }

  // no space found yet
  if (new_space == NULL) {
    //Get more space from OS
    sbrk(size);
    
    //initialize new_space with heap_end
    new_space = heap_end;

    //update heap_end by incrementing the end by size
    heap_end = (void *)((unsigned long long int)heap_end + size);
   
    //Make the current mcb the new space and make it unavailable
    //Then set the mcb to size that you want it.
    cur_mcb = (MCB *)new_space;
    cur_mcb->available = 0;
    cur_mcb->size = size;
    //It is now allocated.
  }

  // new space is after new MCB
  return (void *)( ( long long int )new_space + sizeof( MCB ) );
}

void *malloc_b( long size ) {
  struct MCB *cur_mcb;          // current MCB
  void *new_space = NULL; // this is a pointer to a new memory space allocated for a user

  if( !initialized )   {
    // find the end of heap memory, upon an initialization
    heap_end = sbrk( 0 );
    heap_top = heap_end;
    initialized = true;
  }

  // append an MCB in front of a requested memroy space
  size = size + sizeof( MCB );

  //Make temporary varilables to save the mcb and size
  MCB *bestMCB;
  int bestSize = 0;
  //Scan each mcb from top to bottom of the heap                                                                           
  for(void *cur = heap_top; cur < heap_end; cur =(void *)((unsigned long long int)cur + cur_mcb->size)) {
    //Set current mcb to current but make sure to typecast
    cur_mcb = (MCB *)cur;

    //Check if cur_mcb is available and if the size of the mcb is 
    //greater than or equal to the size that needs to be allocated.               
    if((cur_mcb->available == 1) && (cur_mcb->size >= size)) {

      if(cur_mcb->size == size) {
	//If it is, mark this as best.                 
        bestMCB = cur_mcb;
        bestSize = cur_mcb->size;
	break;
      }
      //Check if best size is zero
      if(bestSize == 0 || cur_mcb->size <= bestSize ){
	bestMCB = cur_mcb;
	bestSize = cur_mcb->size;
      }
    }
  }

  //Check if the best mcb is not null
  if(bestMCB != NULL) {
    //It's filled in now so make it unavailable
    bestMCB->available = 0;

    //Allocate the memory into this space.    
    new_space = bestMCB;
  }

  // no space found yet
  if ( new_space == NULL ) {
    //Get more space from OS
    sbrk(size);

    //initialize new_space with heap_end
    new_space = heap_end;

    //update heap_end by incrementing the end by size
    heap_end = (void *)((unsigned long long int)heap_end + size);

    //Make the current mcb the new space and make it unavailable
    //Then set the mcb to size that you want it.
    cur_mcb = (MCB *)new_space;
    cur_mcb->available = 0;
    cur_mcb->size = size;
    //It is now allocated. 
  }

  // new space is after new MCB
  return (void *)( ( long long int )new_space + sizeof( MCB ) );
}
