// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.cce.report

import com.intellij.cce.metric.MetricInfo
import java.nio.file.Path
import kotlin.io.path.writeText

class JsonReportGenerator(
  outputDir: String,
  filterName: String,
  comparisonFilterName: String,
) : JsonReportGeneratorBase(
  outputDir,
  filterName,
  comparisonFilterName,
  "json"
) {
  override fun generateGlobalReport(globalMetrics: List<MetricInfo>): Path {
    return dir.resolve(metricsInfoName).also {
      it.writeText(gson.toJson(mapOf(
        "metrics" to mapOf(
          "global" to globalMetrics,
          "perFile" to metricPerFile
        ))))
    }
  }

  companion object {
    private const val metricsInfoName = "metrics.json"
  }
}
