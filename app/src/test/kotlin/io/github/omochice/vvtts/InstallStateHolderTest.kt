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
    holder.startRun()

    assertEquals(InstallState.Running(0, 0), holder.current)
  }

  @Test
  fun `progress of a run becomes the current state`() {
    val run = holder.startRun()

    run.progress(10, 100)

    assertEquals(InstallState.Running(10, 100), holder.current)
  }

  @Test
  fun `a run waiting to retry becomes the current state`() {
    val run = holder.startRun()
    run.progress(10, 100)

    run.retrying(2, 5)

    assertEquals(InstallState.Retrying(2, 5), holder.current)
  }

  @Test
  fun `a superseded run waiting to retry does not overwrite the run after it`() {
    val stopped = holder.startRun()
    holder.startRun()

    stopped.retrying(2, 5)

    assertEquals(InstallState.Running(0, 0), holder.current)
  }

  @Test
  fun `the end of a run becomes the current state`() {
    val run = holder.startRun()

    run.finish(InstallResult.Installed)

    assertEquals(InstallState.Finished(InstallResult.Installed), holder.current)
  }

  @Test
  fun `listeners receive every change in order`() {
    val seen = mutableListOf<InstallState>()
    holder.addListener { seen += it }

    val run = holder.startRun()
    run.progress(10, 100)
    run.finish(InstallResult.Installed)

    assertEquals(
        listOf(
            InstallState.Running(0, 0),
            InstallState.Running(10, 100),
            InstallState.Finished(InstallResult.Installed),
        ),
        seen,
    )
  }

  @Test
  fun `a removed listener receives nothing more`() {
    val seen = mutableListOf<InstallState>()
    val listener: (InstallState) -> Unit = { seen += it }
    holder.addListener(listener)
    val run = holder.startRun()

    holder.removeListener(listener)
    run.progress(10, 100)

    assertEquals(listOf<InstallState>(InstallState.Running(0, 0)), seen)
  }

  @Test
  fun `a superseded run that stops late does not overwrite the run after it`() {
    val stopped = holder.startRun()
    val resumed = holder.startRun()
    resumed.progress(10, 100)
    val seen = mutableListOf<InstallState>()
    holder.addListener { seen += it }

    stopped.progress(5, 100)
    stopped.finish(InstallResult.Stopped)

    assertEquals(InstallState.Running(10, 100), holder.current)
    assertEquals(emptyList(), seen)
  }
}
