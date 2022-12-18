package go_concurrency

import "math/rand"

type Matrix struct {
}

func NewMatrix() *Matrix {
	m := Matrix{}
	return &m
}

func (m Matrix) Multiply(A [][]int, B [][]int) [][]int {
	sizeA := len(A)
	sizeB := len(B)
	n := make([][]int, sizeA)
	for i := range n {
		n[i] = make([]int, sizeB)
	}
	for i := 0; i < sizeA; i++ {
		for k := 0; k < sizeB; k++ {
			temp := A[i][k]
			for j := 0; j < sizeB; j++ {
				n[i][j] += temp * B[k][j]
			}
		}
	}
	return n
}

func (m Matrix) SplitMatrix(nrOfThreads int, matrix [][]int) (matrixes [][][]int) {
	splitter := len(matrix) / nrOfThreads
	for i := 0; i < nrOfThreads; i++ {
		matrixes = append(matrixes,
			matrix[splitter*i:(splitter*(i+1))])
	}
	return
}

func (m Matrix) MultiplyStuff(finalMatrix *[][][]int, matrix1 [][]int, matrix2 [][]int, i int) {
	(*finalMatrix)[i] = m.Multiply(matrix1, matrix2)
}

func (m Matrix) GenMatrix() (matrix1 [][]int, matrix2 [][]int) {
	rand.Seed(10)
	matSize := 4096
	matrix1 = make([][]int, matSize)
	for i := 0; i < matSize; i++ {
		matrix1[i] = make([]int, matSize)
		for j := 0; j < matSize; j++ {
			matrix1[i][j] = rand.Int()
		}
	}
	matrix2 = make([][]int, matSize)
	for i := 0; i < matSize; i++ {
		matrix2[i] = make([]int, matSize)
		for j := 0; j < matSize; j++ {
			matrix2[i][j] = rand.Int()
		}
	}
	return
}
