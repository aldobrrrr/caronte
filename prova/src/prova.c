/*
 ============================================================================
 Name        : prova.c
 Author      : aldobrrrr
 Version     :
 Copyright   : Your copyright notice
 Description : Hello World in C, Ansi-style
 ============================================================================
 */

#include <stdio.h>
#include <stdlib.h>

#define N 5
#define NUM_PROD 10
#define NUM_CONS 10
#define TIME_GARBAGE 2

#define EMPTY -101

struct p_lease_v {
	int lease;
	int pos;
	int value;
};

int array[N];

void put(int value, int pos, int lease) {
	int i;
	array[pos] = value;
}

int get(int pos) {
	return array[pos];
}

void clean() {
	int i;
	for (i = 0; i < N; ++i) {
		if (array[i] != EMPTY) {
			array[i] = EMPTY;
		}
	}
}

pthread_mutex_t mutex;
pthread_cond_t cond;

void read(void* pos) {
	int p = (int) pos;
	pthread_mutex_lock(&mutex);
	int res = 0;
	while ((res = get(p)) == EMPTY) {
		pthread_cond_wait(&cond, &mutex);
	}
	pthread_mutex_unlock(&mutex);
	printf("questo è il valore in posizione %d: %d\n", p, res);
}

void prod(void* pos_lease_v) {
	struct p_lease_v* plv = (struct p_lease_v*) pos_lease_v;
	int lease = plv->lease;
	int p = plv->pos;
	int value = plv->value;
	free(plv);
	pthread_mutex_lock(&mutex);
	while (array[p] != EMPTY) {
		pthread_cond_wait(&cond, &mutex);
	}
	put(value, p, lease);
	pthread_mutex_unlock(&mutex);
}

void garbage(void* time_cont) {
	unsigned int t = (unsigned int) time_cont;
	while (1) {
		pthread_mutex_lock(&mutex);
		clean();
		pthread_cond_broadcast(&cond);
		pthread_mutex_unlock(&mutex);
	}
}

int main() {
	int i;
	pthread_t* prods;
	prods = (pthread_t*) malloc(NUM_PROD * sizeof(pthread_t));
	pthread_t* cons;
	pthread_t garb;
	cons = (pthread_t*) malloc(NUM_CONS * sizeof(pthread_t));
	pthread_cond_init(&cond, NULL);

	for (i = 0; i < N; ++i) {
		array[i] = EMPTY;
	}

	pthread_mutex_init(&mutex, NULL);

	struct p_lease_v *plv;
	for (i = 1; i <= NUM_PROD; ++i) {
		plv = (struct p_lease_v *) malloc(sizeof(struct p_lease_v));
		plv->lease = i;
		plv->pos = (i < N) ? i : 5;
		plv->value = i;
		pthread_create(&prods[i], NULL, prod, (void*) plv);
	}

	for (i = 1; i <= NUM_CONS; ++i) {
		pthread_create(&cons[i], NULL, read, (void*) i);
	}

	pthread_create(&garb, NULL, garbage, (void*) TIME_GARBAGE);
	//pthread_exit(&garb);

	return 0;
}
