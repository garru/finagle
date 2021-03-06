package com.twitter.finagle.zipkin.thrift

import com.twitter.finagle.stats.{NullStatsReceiver, StatsReceiver}
import com.twitter.finagle.tracing.{TraceId, Record, Tracer}
import collection.mutable.{SynchronizedMap, HashMap}

object ZipkinTracer {

  // to make sure we only create one instance of the tracer and sampler per host and port
  private[this] val map =
    new HashMap[String, ZipkinTracer] with SynchronizedMap[String, ZipkinTracer]

  val default = mk()

  /**
   * @param scribeHost Host to send trace data to
   * @param scribePort Port to send trace data to
   * @param statsReceiver Where to log information about tracing success/failures
   * @param sampleRate How much data to collect. Default sample rate 0.1%. Max is 1, min 0.
   */
  @deprecated("Use mk() instead", "6.1.0")
  def apply(scribeHost: String = "localhost",
            scribePort: Int = 1463,
            statsReceiver: StatsReceiver = NullStatsReceiver,
            sampleRate: Float = Sampler.DefaultSampleRate
  ): Tracer.Factory = () => mk(scribeHost, scribePort, statsReceiver, sampleRate)

  /**
   * @param host Host to send trace data to
   * @param port Port to send trace data to
   * @param statsReceiver Where to log information about tracing success/failures
   * @param sampleRate How much data to collect. Default sample rate 0.1%. Max is 1, min 0.
   */
  def mk(host: String = "localhost",
         port: Int = 1463,
         statsReceiver: StatsReceiver = NullStatsReceiver,
         sampleRate: Float = Sampler.DefaultSampleRate
  ): Tracer = synchronized {
    val tracer = map.getOrElseUpdate(host + ":" + port, {
      val raw = new RawZipkinTracer(
        host, port, statsReceiver.scope("zipkin"))
      new ZipkinTracer(raw, sampleRate)
    })

    tracer
  }

  /**
   * Util method since named parameters can't be called from Java
   * @param sr stats receiver to send successes/failures to
   */
  @deprecated("Use mk() instead", "6.1.0")
  def apply(sr: StatsReceiver): Tracer.Factory = () => 
    mk("localhost", 1463, sr, Sampler.DefaultSampleRate)

  /**
   * Util method since named parameters can't be called from Java
   * @param sr stats receiver to send successes/failures to
   */
  def mk(statsReceiver: StatsReceiver): Tracer =
    mk("localhost", 1463, statsReceiver, Sampler.DefaultSampleRate)
}

/**
 * Zipkin tracer that supports sampling. Will pass through a small subset of the records.
 * @param underlyingTracer Underlying tracer that accumulates the traces and sends off to the collector.
 * @param initialSampleRate Start off with this sample rate. Can be changed later.
 */
class ZipkinTracer(underlyingTracer: RawZipkinTracer, initialSampleRate: Float) extends Tracer {
  private[this] val sampler = new Sampler
  setSampleRate(initialSampleRate)

  def sampleTrace(traceId: TraceId) = sampler.sampleTrace(traceId)

  def setSampleRate(sampleRate: Float) = sampler.setSampleRate(sampleRate)

  def record(record: Record) {
    if (sampler.sampleRecord(record)) {
      underlyingTracer.record(record)
    }
  }
}
