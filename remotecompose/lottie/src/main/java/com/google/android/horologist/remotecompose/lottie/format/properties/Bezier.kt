/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.horologist.remotecompose.lottie.format.properties

import androidx.compose.remote.creation.compose.state.rb
import com.google.android.horologist.remotecompose.lottie.format.values.BezierValue
import com.google.android.horologist.remotecompose.lottie.format.values.KeyframeEasing
import com.google.android.horologist.remotecompose.lottie.format.values.SerializableRemoteBoolean
import com.google.android.horologist.remotecompose.lottie.format.values.SerializableRemoteFloat
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Base class for all Lottie animatable Bézier properties conforming to
 * [Bézier Property](https://lottie.github.io/lottie-spec/dev/specs/properties/#bezier-property).
 *
 * Bézier properties define cubic polybezier shape paths (used in shapes such as vector paths and
 * masks).
 *
 * Essential Invariants:
 * - The property is partitioned into two mutually exclusive branches identified by the
 *   integer-boolean discriminator [animated]:
 *     - `0` (`false.rb`): [StaticBezierProperty], holding a constant [BezierValue].
 *     - `1` (`true.rb`): [AnimatedBezierProperty], holding a sequence of keyframes over time.
 * - [slotId]: Optional slot identifier (`sid`) enabling runtime value replacement via Lottie slots.
 */
@Serializable(with = BaseBezierPropertySerializer::class)
internal sealed class BaseBezierProperty {
  abstract val animated: SerializableRemoteBoolean
  abstract val slotId: String?
}

/**
 * Conforms to
 * [Bézier Property](https://lottie.github.io/lottie-spec/dev/specs/properties/#bezier-property)
 * (Not animated branch):
 * - Required Fields: `"a"` (const 0), `"k"` (Bézier shape value).
 * - Optional Fields: `"sid"` (slot identifier, default null).
 *
 * Invariants:
 * - [animated] is guaranteed to represent integer `0` (`false.rb`).
 * - [value] contains the constant [BezierValue] geometry.
 */
@Serializable
internal data class StaticBezierProperty(
  @SerialName("sid") override val slotId: String? = null,
  @SerialName("a") override val animated: SerializableRemoteBoolean,
  @SerialName("k") val value: BezierValue,
) : BaseBezierProperty()

/**
 * Conforms to
 * [Bézier Property](https://lottie.github.io/lottie-spec/dev/specs/properties/#bezier-property)
 * (Animated branch):
 * - Required Fields: `"a"` (const 1), `"k"` (array of Bézier keyframes).
 * - Optional Fields: `"sid"` (slot identifier, default null).
 *
 * Invariants:
 * - [animated] is guaranteed to represent integer `1` (`true.rb`).
 * - [keyframes] defines the temporal evolution of the Bézier shape across animation frames.
 */
@Serializable
internal data class AnimatedBezierProperty(
  @SerialName("sid") override val slotId: String? = null,
  @SerialName("a") override val animated: SerializableRemoteBoolean,
  @SerialName("k") val keyframes: List<BezierKeyframe>,
) : BaseBezierProperty()

/**
 * A single Bézier keyframe conforming to
 * [Bézier Keyframe](https://lottie.github.io/lottie-spec/dev/specs/properties/#bezier-keyframe).
 *
 * Defines the Bézier shape value and optional easing interpolation parameters at a specific
 * timeline frame.
 *
 * Schema Specification:
 * - Required Fields: `"t"` (start frame), `"s"` (value array with at least 1 element).
 * - Optional Fields with Schema Default: `"h"` (hold interpolation flag, default: 0 -> `false.rb`).
 * - Optional Fields without Schema Default: `"i"` (incoming tangent), `"o"` (outgoing tangent).
 *
 * Invariants:
 * - [frame]: Timeline time in frames at which this keyframe takes effect.
 * - [value]: Bézier shape values active at [frame]. Must contain at least 1 element per schema.
 * - [hold]: When `1` (`true.rb`), the value is held constant until the next keyframe without
 *   interpolation.
 * - [inTangent], [outTangent]: Optional cubic Bézier easing curve handles conforming to
 *   [Easing Handle](https://lottie.github.io/lottie-spec/dev/specs/properties/#easing-handle).
 *   These are null under any of the following canonical Lottie conditions:
 *     1. Easing handles are omitted from the JSON payload, in which case default linear
 *        interpolation applies.
 *     2. Hold interpolation is active ([hold] is `true.rb`), making easing curves inapplicable.
 *     3. The keyframe is the final (terminal) keyframe in an animation sequence, having no
 *        subsequent interval to interpolate towards.
 */
@Serializable
internal data class BezierKeyframe(
  @SerialName("t") val frame: SerializableRemoteFloat,
  @SerialName("s") val value: List<BezierValue>,
  @SerialName("h") val hold: SerializableRemoteBoolean = false.rb,
  @SerialName("i") val inTangent: KeyframeEasing? = null,
  @SerialName("o") val outTangent: KeyframeEasing? = null,
) {
  init {
    if (value.isEmpty()) {
      throw SerializationException(
        "Keyframe 's' array must not be empty per Lottie schema (minItems: 1)"
      )
    }
  }
}

/**
 * Polymorphic serializer for [BaseBezierProperty] discriminating between static and animated
 * variants based on the Lottie schema `"a"` field ([Integer
 * Boolean](https://lottie.github.io/lottie-spec/dev/specs/values/#int-boolean)).
 *
 * Contract:
 * - Preconditions: [element] must be a [JsonObject].
 * - Postconditions:
 *     - Selects [AnimatedBezierProperty.serializer] when `"a"` is integer `1`.
 *     - Selects [StaticBezierProperty.serializer] when `"a"` is integer `0`.
 * - Exceptions:
 *     - Throws [SerializationException] if [element] is not a [JsonObject].
 *     - Throws [SerializationException] if `"a"` is missing or cannot be parsed as an integer
 *       boolean.
 *     - Throws [SerializationException] if `"a"` is neither `0` nor `1`.
 */
internal object BaseBezierPropertySerializer :
  JsonContentPolymorphicSerializer<BaseBezierProperty>(BaseBezierProperty::class) {
  override fun selectDeserializer(
    element: JsonElement
  ): DeserializationStrategy<BaseBezierProperty> {
    val obj = element as? JsonObject ?: throw SerializationException("Expected JSON object")
    val animated = obj["a"]?.jsonPrimitive?.intOrNull
    return when (animated) {
      1 -> AnimatedBezierProperty.serializer()
      0 -> StaticBezierProperty.serializer()
      null ->
        throw SerializationException("Bezier property missing required 'a' field per Lottie schema")
      else -> throw SerializationException("Field 'a' must be 0 or 1, but was $animated")
    }
  }
}

/** Serializer for [StaticBezierProperty] supporting slot IDs and raw bezier values. */
internal object StaticBezierPropertySerializer : KSerializer<StaticBezierProperty> {
  override val descriptor: SerialDescriptor =
    buildClassSerialDescriptor("StaticBezierProperty") {
      element<String?>("sid", isOptional = true)
      element<Boolean>("animated", isOptional = true)
      element<BezierValue>("k")
    }

  override fun deserialize(decoder: Decoder): StaticBezierProperty {
    val jsonDecoder = decoder as JsonDecoder
    val element = jsonDecoder.decodeJsonElement()
    return when (element) {
      is JsonObject -> {
        val slotId = element["sid"]?.jsonPrimitive?.contentOrNull
        val kElem = element["k"]
        val bezierValue =
          if (kElem != null) {
            jsonDecoder.json.decodeFromJsonElement(BezierValueSerializer, kElem)
          } else {
            jsonDecoder.json.decodeFromJsonElement(BezierValueSerializer, element)
          }
        StaticBezierProperty(slotId = slotId, animated = false, value = bezierValue)
      }
      else -> {
        val bezierValue = jsonDecoder.json.decodeFromJsonElement(BezierValueSerializer, element)
        StaticBezierProperty(slotId = null, animated = false, value = bezierValue)
      }
    }
  }

  override fun serialize(encoder: Encoder, value: StaticBezierProperty) {
    val jsonEncoder = encoder as JsonEncoder
    jsonEncoder.encodeJsonElement(
      buildJsonObject {
        value.slotId?.let { put("sid", it) }
        put("a", 0)
        put("k", jsonEncoder.json.encodeToJsonElement(BezierValueSerializer, value.value))
      }
    )
  }
}

/** Serializer for [AnimatedBezierProperty] supporting slot IDs and keyframes. */
internal object AnimatedBezierPropertySerializer : KSerializer<AnimatedBezierProperty> {
  override val descriptor: SerialDescriptor =
    buildClassSerialDescriptor("AnimatedBezierProperty") {
      element<String?>("sid", isOptional = true)
      element<Int>("a")
      element<List<BezierPropertyKeyframe>>("k")
    }

  override fun deserialize(decoder: Decoder): AnimatedBezierProperty {
    val jsonDecoder = decoder as JsonDecoder
    val obj = jsonDecoder.decodeJsonElement().jsonObject
    val slotId = obj["sid"]?.jsonPrimitive?.contentOrNull
    val animatedInt = obj["a"]?.jsonPrimitive?.intOrNull ?: 1
    val keyframesArray = obj["k"]?.jsonArray
    val keyframes =
      keyframesArray?.map { element ->
        jsonDecoder.json.decodeFromJsonElement(BezierPropertyKeyframeSerializer, element)
      } ?: emptyList()
    return AnimatedBezierProperty(slotId = slotId, animatedInt = animatedInt, keyframes = keyframes)
  }

  override fun serialize(encoder: Encoder, value: AnimatedBezierProperty) {
    val jsonEncoder = encoder as JsonEncoder
    jsonEncoder.encodeJsonElement(
      buildJsonObject {
        value.slotId?.let { put("sid", it) }
        put("a", value.animatedInt)
        put(
          "k",
          jsonEncoder.json.encodeToJsonElement(
            ListSerializer(BezierPropertyKeyframeSerializer),
            value.keyframes,
          ),
        )
      }
    )
  }
}

/** Serializer for [BezierPropertyKeyframe] handling timing, easing, and flexible shape values. */
internal object BezierPropertyKeyframeSerializer : KSerializer<BezierPropertyKeyframe> {
  override val descriptor: SerialDescriptor =
    buildClassSerialDescriptor("BezierPropertyKeyframe") {
      element<Float>("t", isOptional = true)
      element<Boolean>("h", isOptional = true)
      element<ScalarKeyframeEasing?>("i", isOptional = true)
      element<ScalarKeyframeEasing?>("o", isOptional = true)
      element<List<BezierValue>>("s", isOptional = true)
    }

  override fun deserialize(decoder: Decoder): BezierPropertyKeyframe {
    val jsonDecoder = decoder as JsonDecoder
    val obj = jsonDecoder.decodeJsonElement().jsonObject

    val frame = obj["t"]?.jsonPrimitive?.floatOrNull ?: 0f
    val hold =
      when (val hElem = obj["h"]) {
        is JsonPrimitive -> hElem.booleanOrNull ?: ((hElem.intOrNull ?: 0) == 1)
        else -> false
      }
    val inTangent =
      obj["i"]?.let { jsonDecoder.json.decodeFromJsonElement(ScalarKeyframeEasingSerializer, it) }
    val outTangent =
      obj["o"]?.let { jsonDecoder.json.decodeFromJsonElement(ScalarKeyframeEasingSerializer, it) }
    val sElem = obj["s"]
    val value =
      when (sElem) {
        is JsonArray ->
          sElem.map { jsonDecoder.json.decodeFromJsonElement(BezierValueSerializer, it) }
        is JsonObject ->
          listOf(jsonDecoder.json.decodeFromJsonElement(BezierValueSerializer, sElem))
        else -> emptyList()
      }

    return BezierPropertyKeyframe(
      frame = frame,
      hold = hold,
      inTangent = inTangent,
      outTangent = outTangent,
      value = value,
    )
  }

  override fun serialize(encoder: Encoder, value: BezierPropertyKeyframe) {
    val jsonEncoder = encoder as JsonEncoder
    jsonEncoder.encodeJsonElement(
      buildJsonObject {
        put("t", value.frame)
        if (value.hold) put("h", 1)
        value.inTangent?.let {
          put("i", jsonEncoder.json.encodeToJsonElement(ScalarKeyframeEasingSerializer, it))
        }
        value.outTangent?.let {
          put("o", jsonEncoder.json.encodeToJsonElement(ScalarKeyframeEasingSerializer, it))
        }
        put(
          "s",
          jsonEncoder.json.encodeToJsonElement(ListSerializer(BezierValueSerializer), value.value),
        )
      }
    )
  }
}
