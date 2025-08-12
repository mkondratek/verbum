package com.kondratek.verbum.demo

import com.mkondratek.verbum.indexer.VerbumIndexer
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Paths

data class PathRequest(
    val path: String,
)

@RestController
@RequestMapping("/api/indexer")
class IndexerController(
    private val verbumIndexer: VerbumIndexer,
) {
    @PostMapping("/add")
    fun addPath(
        @RequestBody request: PathRequest,
    ): ResponseEntity<String> =
        try {
            verbumIndexer.addPath(Paths.get(request.path))
            ResponseEntity.ok("Path added and indexed: $request")
        } catch (e: Exception) {
            ResponseEntity.badRequest().body("Failed to add path: ${e.message}")
        }

    @PostMapping("/remove")
    fun removePath(
        @RequestBody request: PathRequest,
    ): ResponseEntity<String> =
        try {
            verbumIndexer.removePath(Paths.get(request.path))
            ResponseEntity.ok("Path removed and deindexed: $request")
        } catch (e: Exception) {
            ResponseEntity.badRequest().body("Failed to remove path: ${e.message}")
        }

    @GetMapping("/query")
    fun query(
        @RequestParam word: String,
    ): ResponseEntity<Set<String>> {
        val results = verbumIndexer.query(word).map { it.toString() }.toSet()
        return ResponseEntity.ok(results)
    }

    @PostMapping("/start")
    fun startWatching(): ResponseEntity<String> =
        try {
            verbumIndexer.startWatching()
            ResponseEntity.ok("Indexing started")
        } catch (e: Exception) {
            ResponseEntity.badRequest().body("Failed to start watching: ${e.message}")
        }

    @PostMapping("/stop")
    fun stopWatching(): ResponseEntity<String> =
        try {
            verbumIndexer.stopWatching()
            ResponseEntity.ok("Indexing stopped")
        } catch (e: Exception) {
            ResponseEntity.badRequest().body("Failed to stop watching: ${e.message}")
        }
}
