# Documentation Index

Router catalog for Lottie Remote Compose documentation.

## Document Directory

| Document | Type | Description |
|---|---|---|
| [`_framework.md`](_framework.md) | Map | Architectural framework map and module layers |
| [`_glossary.md`](_glossary.md) | Glossary | Ubiquitous project terms and domain concepts |
| [`format.concept.md`](format.concept.md) | Concept | Lottie AST format and serialization layer |
| [`format.sp.md`](format.sp.md) | Specification | Format serialization contracts and schema rules |
| [`format.plan.md`](format.plan.md) | Plan | Format layer roadmap |
| [`renderer.concept.md`](renderer.concept.md) | Concept | Remote Compose rendering and geometry engine |
| [`renderer.sp.md`](renderer.sp.md) | Specification | Renderer contracts and path evaluation rules |
| [`renderer.plan.md`](renderer.plan.md) | Plan | Renderer layer roadmap |
| [`root_compose.concept.md`](root_compose.concept.md) | Concept | Public Composable APIs and context injection |
| [`root_compose.sp.md`](root_compose.sp.md) | Specification | Root Compose composable contracts |
| [`root_compose.plan.md`](root_compose.plan.md) | Plan | Root Compose execution plan |
| [`shapes.concept.md`](shapes.concept.md) | Concept | Graphic elements & shapes architecture (Lottie 1.0.1) |
| [`shapes.sp.md`](shapes.sp.md) | Specification | Graphic elements data structures and rendering rules |
| [`shapes.plan.md`](shapes.plan.md) | Plan | Graphic elements modularization & Stroke support plan |
| [`shapes_fixes.plan.md`](shapes_fixes.plan.md) | Plan | Graphic elements parity and modifier fixes plan |
| [`layers.concept.md`](layers.concept.md) | Concept | Layers architecture & modular structure (Lottie 1.0.1) |
| [`layers.sp.md`](layers.sp.md) | Specification | Layers data structures and rendering contracts |
| [`layers.plan.md`](layers.plan.md) | Plan | Layers modularization & timing compliance plan |
| [`scalar.plan.md`](scalar.plan.md) | Plan | Scalar property compliance and animation plan |
| [`vector.plan.md`](vector.plan.md) | Plan | Vector property compliance and animation plan |
| [`position.plan.md`](position.plan.md) | Plan | Position property compliance and animation plan |
| [`color.plan.md`](color.plan.md) | Plan | Color property compliance and animation plan |
| [`bezier.plan.md`](bezier.plan.md) | Plan | Bezier property compliance and animation plan |
| [`gradient.plan.md`](gradient.plan.md) | Plan | Gradient property compliance and animation plan |
| [`stroke.plan.md`](stroke.plan.md) | Plan | Legacy stroke plan (superseded by shapes.plan.md) |
| [`lottie_spec.spike.md`](lottie_spec.spike.md) | Spike | Time-boxed exploration of Lottie 1.0.1 specification |
| [`screenshot_diff.concept.md`](screenshot_diff.concept.md) | Concept | Lottie screenshot diff testing framework & multi-progress validation |
| [`screenshot_diff.sp.md`](screenshot_diff.sp.md) | Specification | Screenshot diff test harness contracts & keyframe matrix |
| [`screenshot_diff.plan.md`](screenshot_diff.plan.md) | Plan | Multi-progress screenshot diff test implementation plan |
| [`track_matte_and_styles.concept.md`](track_matte_and_styles.concept.md) | Concept | Track matte layer masking & container shape style resolution |
| [`track_matte_and_styles.sp.md`](track_matte_and_styles.sp.md) | Specification | Track matte pairing, canvas clipping & style scope contracts |
| [`track_matte_and_styles.plan.md`](track_matte_and_styles.plan.md) | Plan | Track matte and container style resolution implementation plan |
| [`group_transforms_and_styles.plan.md`](group_transforms_and_styles.plan.md) | Plan | Group geometry transformations and container style parity plan |
| [`rectangle_rounded_corners.plan.md`](rectangle_rounded_corners.plan.md) | Plan | Parametric rectangle rounded corners dynamic evaluation plan |
| [`lottie_audit_and_gap_analysis.md`](lottie_audit_and_gap_analysis.md) | Audit | Lottie 1.0.1 specification compliance audit and commit regression analysis |
| [`lottie_spec_parity.plan.md`](lottie_spec_parity.plan.md) | Plan | Lottie 1.0.1 specification parity and hardening plan |
| [`sample_lottie_gallery.concept.md`](sample_lottie_gallery.concept.md) | Concept | Wear OS Demo Sample App Lottie Showcase & Device Testing Gallery concept |
| [`sample_lottie_gallery.sp.md`](sample_lottie_gallery.sp.md) | Specification | Sample Lottie showcase data structures, contracts & inspection rules |
| [`sample_lottie_gallery.plan.md`](sample_lottie_gallery.plan.md) | Plan | Sample Lottie showcase implementation plan |
| [`lottie_remediation.concept.md`](lottie_remediation.concept.md) | Concept | Lottie 1.0.1 specification parity remediation & hardening concept |
| [`lottie_remediation.sp.md`](lottie_remediation.sp.md) | Specification | Lottie remediation contracts, math formulas & verification criteria |
| [`lottie_remediation.plan.md`](lottie_remediation.plan.md) | Plan | Lottie 1.0.1 specification parity remediation implementation plan |

## ID Prefix Mapping

| Prefix | Document |
|---|---|
| `DOC_LOTTIE_AUDIT` | [`lottie_audit_and_gap_analysis.md`](lottie_audit_and_gap_analysis.md) |
| `PL_LOTTIE_SPEC_PARITY` | [`lottie_spec_parity.plan.md`](lottie_spec_parity.plan.md) |
| `C_SAMPLE_LOTTIE` | [`sample_lottie_gallery.concept.md`](sample_lottie_gallery.concept.md) |
| `SP_SAMPLE_LOTTIE` | [`sample_lottie_gallery.sp.md`](sample_lottie_gallery.sp.md) |
| `PL_SAMPLE_LOTTIE` | [`sample_lottie_gallery.plan.md`](sample_lottie_gallery.plan.md) |
| `C_LOTTIE_REMED` | [`lottie_remediation.concept.md`](lottie_remediation.concept.md) |
| `SP_LOTTIE_REMED` | [`lottie_remediation.sp.md`](lottie_remediation.sp.md) |
| `PL_LOTTIE_REMED` | [`lottie_remediation.plan.md`](lottie_remediation.plan.md) |
| `C_FMT` | [`format.concept.md`](format.concept.md) |

| `SP_FMT` | [`format.sp.md`](format.sp.md) |
| `PL_FMT` | [`format.plan.md`](format.plan.md) |
| `C_RND` | [`renderer.concept.md`](renderer.concept.md) |
| `SP_RND` | [`renderer.sp.md`](renderer.sp.md) |
| `PL_RND` | [`renderer.plan.md`](renderer.plan.md) |
| `C_LOTTIE_SHAPES` | [`shapes.concept.md`](shapes.concept.md) |
| `SP_LOTTIE_SHAPES` | [`shapes.sp.md`](shapes.sp.md) |
| `PL_LOTTIE_SHAPES` | [`shapes.plan.md`](shapes.plan.md) |
| `PL_LOTTIE_SHAPES_FIXES` | [`shapes_fixes.plan.md`](shapes_fixes.plan.md) |
| `PL_LOTTIE_RECTANGLE_ROUNDED_CORNERS` | [`rectangle_rounded_corners.plan.md`](rectangle_rounded_corners.plan.md) |
| `PL_LOTTIE_GROUP_TRANSFORM` | [`group_transforms_and_styles.plan.md`](group_transforms_and_styles.plan.md) |
| `C_LOTTIE_LAYERS` | [`layers.concept.md`](layers.concept.md) |
| `SP_LOTTIE_LAYERS` | [`layers.sp.md`](layers.sp.md) |
| `PL_LOTTIE_LAYERS` | [`layers.plan.md`](layers.plan.md) |
| `PL_LOTTIE_SCALAR` | [`scalar.plan.md`](scalar.plan.md) |
| `PL_LOTTIE_VECTOR` | [`vector.plan.md`](vector.plan.md) |
| `PL_LOTTIE_POSITION` | [`position.plan.md`](position.plan.md) |
| `PL_LOTTIE_COLOR` | [`color.plan.md`](color.plan.md) |
| `PL_LOTTIE_BEZIER` | [`bezier.plan.md`](bezier.plan.md) |
| `PL_LOTTIE_GRADIENT` | [`gradient.plan.md`](gradient.plan.md) |
| `PL_LOTTIE_STROKE` | [`stroke.plan.md`](stroke.plan.md) |
| `C_LOTTIE_DIFF` | [`screenshot_diff.concept.md`](screenshot_diff.concept.md) |
| `SP_LOTTIE_DIFF` | [`screenshot_diff.sp.md`](screenshot_diff.sp.md) |
| `PL_LOTTIE_DIFF` | [`screenshot_diff.plan.md`](screenshot_diff.plan.md) |
| `C_LOTTIE_MATTE` | [`track_matte_and_styles.concept.md`](track_matte_and_styles.concept.md) |
| `SP_LOTTIE_MATTE` | [`track_matte_and_styles.sp.md`](track_matte_and_styles.sp.md) |
| `PL_LOTTIE_MATTE` | [`track_matte_and_styles.plan.md`](track_matte_and_styles.plan.md) |
