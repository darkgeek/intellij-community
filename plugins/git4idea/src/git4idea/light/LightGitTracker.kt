// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package git4idea.light

import com.intellij.ide.FrameStateListener
import com.intellij.ide.lightEdit.LightEditService
import com.intellij.ide.lightEdit.LightEditorInfo
import com.intellij.ide.lightEdit.LightEditorListener
import com.intellij.ide.lightEdit.LightEditorManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.EventDispatcher
import com.intellij.vcs.log.BaseSingleTaskController
import git4idea.config.GitExecutableManager
import java.util.*

internal class LightGitTracker(private val lightEditService: LightEditService) : Disposable {
  private val lightEditorManager: LightEditorManager = lightEditService.editorManager
  private val eventDispatcher = EventDispatcher.create(LightGitTrackerListener::class.java)
  private val singleTaskController = MySingleTaskController()
  private val listener = MyLightEditorListener()

  private val gitExecutable: String
    get() = GitExecutableManager.getInstance().pathToGit

  var currentLocation: String? = null

  init {
    lightEditorManager.addListener(listener, this)
    ApplicationManager.getApplication().messageBus.connect(this).subscribe(FrameStateListener.TOPIC,
                                                                           MyFrameStateListener())
  }

  private fun updateCurrentLocation(location: Optional<String>) {
    currentLocation = location.orElse(null)
    eventDispatcher.multicaster.update()
  }

  private fun update(file: VirtualFile?) {
    clear()
    if (file != null) {
      singleTaskController.request(file)
    }
  }

  private fun clear() {
    updateCurrentLocation(Optional.ofNullable(null))
  }

  fun addUpdateListener(listener: LightGitTrackerListener, parent: Disposable) {
    eventDispatcher.addListener(listener, parent)
  }

  override fun dispose() {
  }

  private inner class MyFrameStateListener : FrameStateListener {
    override fun onFrameActivated() {
      update(lightEditService.selectedFile)
    }
  }

  private inner class MyLightEditorListener : LightEditorListener {
    override fun afterSelect(editorInfo: LightEditorInfo?) {
      update(editorInfo?.file)
    }
  }

  private inner class MySingleTaskController :
    BaseSingleTaskController<VirtualFile, Optional<String>>("Light Git Tracker", this::updateCurrentLocation, this) {
    override fun process(requests: List<VirtualFile>): Optional<String> {
      try {
        return Optional.of(getLocation(requests.last().parent, gitExecutable))
      }
      catch (_: VcsException) {
        return Optional.ofNullable(null)
      }
    }
  }
}

interface LightGitTrackerListener : EventListener {
  fun update()
}