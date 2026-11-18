package io.github.omochice.vvtts

import io.github.omochice.vvtts.assets.InstallResult
import kotlin.test.Test
import kotlin.test.assertEquals

class InstallStateHolderTest {
  private val holder = InstallStateHolder()

  @Test
  fun `a new holder is idle`() {
    assertEquals(InstallState.Idle, holder.current)
  }

  @Test
  fun `a started run is running with nothing known yet`() {
    holder.startRun("0.vvm")

    assertEquals(InstallState.Running("0.vvm", 0, 0), holder.current)
  }

  @Test
  fun `progress of a run becomes the current state`() {
    val run = holder.startRun("0.vvm")

    run.progress(10, 100)

    assertEquals(InstallState.Running("0.vvm", 10, 100), holder.current)
  }

  @Test
  fun `a run waiting to retry becomes the current state`() {
    val run = holder.startRun("0.vvm")
    run.progress(10, 100)

    run.retrying(2, 5)

    assertEquals(InstallState.Retrying("0.vvm", 2, 5), holder.current)
  }

  @Test
  fun `a superseded run waiting to retry does not overwrite the run after it`() {
    val stopped = holder.startRun("0.vvm")
    holder.startRun("5.vvm")

    stopped.retrying(2, 5)

    assertEquals(InstallState.Running("5.vvm", 0, 0), holder.current)
  }

  @Test
  fun `the end of a run becomes the current state`() {
    val run = holder.startRun("0.vvm")

    run.finish(InstallResult.Installed)

    assertEquals(InstallState.Finished("0.vvm", InstallResult.Installed), holder.current)
  }

  @Test
  fun `listeners receive every change in order`() {
    val seen = mutableListOf<InstallState>()
    holder.addListener { seen += it }

    val run = holder.startRun("0.vvm")
    run.progress(10, 100)
    run.finish(InstallResult.Installed)

    assertEquals(
        listOf(
            InstallState.Running("0.vvm", 0, 0),
            InstallState.Running("0.vvm", 10, 100),
            InstallState.Finished("0.vvm", InstallResult.Installed),
        ),
        seen,
    )
  }

  @Test
  fun `a removed listener receives nothing more`() {
    val seen = mutableListOf<InstallState>()
    val listener: (InstallState) -> Unit = { seen += it }
    holder.addListener(listener)
    val run = holder.startRun("0.vvm")

    holder.removeListener(listener)
    run.progress(10, 100)

    assertEquals(listOf<InstallState>(InstallState.Running("0.vvm", 0, 0)), seen)
  }

  @Test
  fun `every state of a run names the voice model it installs`() {
    val seen = mutableListOf<InstallState>()
    holder.addListener { seen += it }

    val run = holder.startRun("5.vvm")
    run.progress(10, 100)
    run.retrying(2, 5)
    run.finish(InstallResult.Installed)

    assertEquals(
        listOf(
            InstallState.Running("5.vvm", 0, 0),
            InstallState.Running("5.vvm", 10, 100),
            InstallState.Retrying("5.vvm", 2, 5),
            InstallState.Finished("5.vvm", InstallResult.Installed),
        ),
        seen,
    )
  }

  @Test
  fun `a superseded run that stops late does not overwrite the run after it`() {
    val stopped = holder.startRun("0.vvm")
    val resumed = holder.startRun("0.vvm")
    resumed.progress(10, 100)
    val seen = mutableListOf<InstallState>()
    holder.addListener { seen += it }

    stopped.progress(5, 100)
    stopped.finish(InstallResult.Stopped)

    assertEquals(InstallState.Running("0.vvm", 10, 100), holder.current)
    assertEquals(emptyList(), seen)
  }
}
