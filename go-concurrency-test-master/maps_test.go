package go_concurrency_test

import (
	"fmt"
	"go_concurrency"
	"sync"
	"sync/atomic"
	"testing"
	"time"
)

const NGOS = 2 // number of concurrent go routines for read/load tests
const Mask = (1024 * 1024) - 1

var um = go_concurrency.NewUnsharedCache()
var lm = go_concurrency.NewLockCache()
var sm = go_concurrency.NewSyncCache()
var cm = go_concurrency.NewChannelCache()
var sc = go_concurrency.NewShardCache()
var ssc = go_concurrency.NewSharedShardCache()
var im = go_concurrency.NewIntMap(256000)         // so there are 4x collisions
var im2 = go_concurrency.NewIntMap(1000000)       // so there are no collisions
var sim = go_concurrency.NewSharedIntMap(1000000) // so there are no collisions

var Sink atomic.Value

func rand(r int) int {
	/* Algorithm "xor" from p. 4 of Marsaglia, "Xorshift RNGs" */
	r ^= r << 13
	r ^= r >> 17
	r ^= r << 5
	return r & 0x7fffffff
}

func TestNewSharedIntMap(t *testing.T) {
	for i := 0; i < 2000000; i++ {
		sim.Put(i, i)
	}
	for i := 0; i < 2000000; i++ {
		if sim.Get(i) != i {
			t.Fatal("wrong value")
		}
	}
}

func BenchmarkRand(m *testing.B) {
	r := time.Now().Nanosecond()
	for i := 0; i < m.N; i++ {
		r = rand(r)
	}
	Sink.Store(r)
}

func testget(impl go_concurrency.Cache, b *testing.B) {
	r := time.Now().Nanosecond()

	var sum int
	for i := 0; i < b.N; i++ {
		r = rand(r)
		sum += impl.Get(r & Mask)
	}
	Sink.Store(sum)
}

func testput(impl go_concurrency.Cache, b *testing.B) {
	r := time.Now().Nanosecond()
	for i := 0; i < b.N; i++ {
		r = rand(r)
		impl.Put(r&Mask, r)
	}
}

func testputget(impl go_concurrency.Cache, b *testing.B) {
	r := time.Now().Nanosecond()
	var sum int
	for i := 0; i < b.N; i++ {
		r = rand(r)
		impl.Put(r&Mask, r)
		r = rand(r)
		sum += impl.Get(r & Mask)
	}
	Sink.Store(sum)
}

func BenchmarkMain(m *testing.B) {
	fmt.Println("populating maps...")
	for i := 0; i <= Mask; i++ {
		um.Put(i, i)
		lm.Put(i, i)
		sm.Put(i, i)
		cm.Put(i, i)
		sc.Put(i, i)
		ssc.Put(i, i)
		im.Put(i, i)
		im2.Put(i, i)
		sim.Put(i, i)
	}

	sm.Get(100)
	m.ResetTimer()

	impls := []go_concurrency.Cache{um, lm, sm, im, im2}
	names := []string{"unshared", "lock", "sync", "intmap", "intmap2"}

	for i := 0; i < len(impls); i++ {
		impl := impls[i]
		m.Run(names[i]+".get", func(b *testing.B) {
			testget(impl, b)
		})
	}
	for i := 0; i < len(impls); i++ {
		impl := impls[i]
		m.Run(names[i]+".put", func(b *testing.B) {
			testput(impl, b)
		})
	}
	for i := 0; i < len(impls); i++ {
		impl := impls[i]
		m.Run(names[i]+".putget", func(b *testing.B) {
			testputget(impl, b)
		})
	}
	for i := 0; i < len(impls); i++ {
		impl := impls[i]
		m.Run(names[i]+".multiget", func(b *testing.B) {
			wg := sync.WaitGroup{}
			for g := 0; g < NGOS; g++ {
				wg.Add(1)
				go func() {
					testget(impl, b)
					wg.Done()
				}()
			}
			wg.Wait()
		})
	}
	for i := 1; i < len(impls); i++ {
		impl := impls[i]
		m.Run(names[i]+".multiput", func(b *testing.B) {
			wg := sync.WaitGroup{}
			for g := 0; g < NGOS; g++ {
				wg.Add(1)
				go func() {
					testput(impl, b)
					wg.Done()
				}()
			}
			wg.Wait()
		})
	}
	for i := 1; i < len(impls); i++ {
		impl := impls[i]
		m.Run(names[i]+".multiputget", func(b *testing.B) {
			wg := sync.WaitGroup{}
			for g := 0; g < NGOS; g++ {
				wg.Add(1)
				go func() {
					testputget(impl, b)
					wg.Done()
				}()
			}
			wg.Wait()
		})
	}
}
