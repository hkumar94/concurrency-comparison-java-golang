package go_concurrency_test

import (
	"fmt"
	"go_concurrency"
	"sync"
	"testing"
)

var TNGOS int // number of concurrent go routines for read/load tests

var mat = go_concurrency.NewMatrix()

func BenchmarkMain2(m *testing.B) {
	fmt.Println("starting benchmark...")

	m.ResetTimer()

	thrd := []int{1, 2, 4, 8, 16}
	names := []string{"1-Thread", "2-Thread", "4-Thread", "8-Thread", "16-Thread"}

	for i := 0; i < len(names); i++ {
		TNGOS = thrd[i]

		m.Run("TestMatrix."+names[i], func(b *testing.B) {
			wg := sync.WaitGroup{}
			matrix1, matrix2 := mat.GenMatrix()
			if TNGOS == 1 {
				for g := 0; g < TNGOS; g++ {
					wg.Add(1)
					go func() {
						mat.Multiply(matrix1, matrix2)
						wg.Done()
					}()
				}
			} else {
				finishedMatrix := make([][][]int, TNGOS)
				matrixes := mat.SplitMatrix(TNGOS, matrix1)
				for g := 0; g < TNGOS; g++ {
					wg.Add(1)
					go func(index int) {
						mat.MultiplyStuff(&finishedMatrix, matrixes[index], matrix2, index)
						wg.Done()
					}(i)
				}
			}
			wg.Wait()
		})
	}
}
