
bl_info = {
    "name": "AnimaTruc Blender I/O",
    "author": "Hoyatla",
    "version": (1, 0, 0),
    "blender": (4, 0, 0),
    "location": "File > Import/Export, View3D > Sidebar > AnimaTruc",
    "description": "Import and export AnimaTruc runtime packs directly from Blender.",
    "category": "Import-Export",
}


import json
import math
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Sequence, Tuple

import bpy
from bpy.props import BoolProperty, FloatProperty, IntProperty, StringProperty
from bpy.types import Operator, Panel
from bpy_extras.io_utils import ExportHelper, ImportHelper
from mathutils import Matrix, Quaternion, Vector

ADDON_ID = "animatruc_blender_io"
PACK_VERSION = 2
DEFAULT_TICKS_PER_SECOND = 20
DEFAULT_BONE_LENGTH = 0.25
FLOAT_EPSILON = 1.0e-6
PACK_NAME_SUFFIXES = (
    ".animatrucpack.json",
    ".animatrucpack",
    ".animatruc.json",
    ".animatruc",
    ".json",
)


@dataclass
class ExportOptions:
    filepath: str
    pack_unit_scale: float = 1.0
    ticks_per_second: int = DEFAULT_TICKS_PER_SECOND
    selection_only: bool = False
    apply_modifiers: bool = True
    triangulate_meshes: bool = False
    export_actions: bool = True


@dataclass
class ImportOptions:
    filepath: str
    pack_unit_scale: float = 1.0
    ticks_per_second: int = DEFAULT_TICKS_PER_SECOND
    create_actions: bool = True
    create_collection: bool = True


@dataclass
class ExportSummary:
    warnings: List[str] = field(default_factory=list)
    meshes_exported: int = 0
    clips_exported: int = 0


@dataclass
class ImportSummary:
    warnings: List[str] = field(default_factory=list)
    meshes_imported: int = 0
    cubes_imported: int = 0
    clips_imported: int = 0


@dataclass
class BoneSpec:
    name: str
    parent: Optional[str]
    pivot: Vector
    local_rotation: Quaternion
    local_scale: Vector
    length: float


def _float(value: float) -> float:
    return float(value)


def _vec3(values: Sequence[float]) -> Vector:
    return Vector((float(values[0]), float(values[1]), float(values[2])))


def _vec3_list(vector: Vector) -> List[float]:
    return [_float(vector.x), _float(vector.y), _float(vector.z)]


def _quat_list(quaternion: Quaternion) -> List[float]:
    q = quaternion.normalized()
    return [_float(q.x), _float(q.y), _float(q.z), _float(q.w)]


def _quat_from_pack(value: Sequence[float]) -> Quaternion:
    if len(value) >= 4:
        return Quaternion((float(value[3]), float(value[0]), float(value[1]), float(value[2]))).normalized()
    return Quaternion((1.0, 0.0, 0.0, 0.0))


def _identity_bind_pose() -> Dict[str, object]:
    return {
        "translation": [0.0, 0.0, 0.0],
        "rotation": [0.0, 0.0, 0.0, 1.0],
        "scale": [1.0, 1.0, 1.0],
    }


def _effective_fps(scene: bpy.types.Scene) -> float:
    fps_base = scene.render.fps_base if scene.render.fps_base else 1.0
    fps = scene.render.fps / fps_base
    return fps if fps > 0.0 else 24.0


def _sanitize_project_name(value: str) -> str:
    base = (value or "animatruc_scene").strip()
    if not base:
        return "animatruc_scene"
    return "_".join(base.split())


def _strip_pack_suffixes(file_name: str) -> str:
    stripped = str(file_name or "").strip()
    changed = True

    while changed and stripped:
        changed = False
        lowered = stripped.lower()
        for suffix in PACK_NAME_SUFFIXES:
            if lowered.endswith(suffix):
                stripped = stripped[:-len(suffix)]
                changed = True
                break

    return stripped


def _canonical_animatruc_json_filepath(filepath: str) -> str:
    path = Path(filepath)
    parent = path.parent if str(path.parent) not in {"", "."} else Path.cwd()
    base_name = _strip_pack_suffixes(path.name)
    sanitized = _sanitize_project_name(base_name)
    return str(parent / f"{sanitized}.animatruc.json")


def _sorted_bones_parent_first(bones: Iterable[bpy.types.Bone]) -> List[bpy.types.Bone]:
    remaining = {bone.name: bone for bone in bones}
    ordered: List[bpy.types.Bone] = []

    while remaining:
        progressed = False
        for bone_name, bone in list(remaining.items()):
            parent_name = bone.parent.name if bone.parent else None
            if parent_name is None or parent_name not in remaining:
                ordered.append(bone)
                remaining.pop(bone_name)
                progressed = True
        if not progressed:
            ordered.extend(remaining.values())
            break

    return ordered

def _extract_bone_name(data_path: str) -> Optional[str]:
    marker = 'pose.bones["'
    if marker not in data_path:
        return None
    start = data_path.index(marker) + len(marker)
    end = data_path.find('"]', start)
    if end < 0:
        return None
    return data_path[start:end]


def _channel_from_data_path(data_path: str) -> Optional[str]:
    if data_path.endswith("location"):
        return "translation"
    if data_path.endswith("rotation_quaternion") or data_path.endswith("rotation_euler") or data_path.endswith("rotation_axis_angle"):
        return "rotation"
    if data_path.endswith("scale"):
        return "scale"
    return None


def _unique_collection_name(base_name: str) -> str:
    existing = set(bpy.data.collections.keys())
    if base_name not in existing:
        return base_name

    index = 1
    while True:
        candidate = f"{base_name}_{index}"
        if candidate not in existing:
            return candidate
        index += 1


def _ensure_object_mode(context: bpy.types.Context) -> None:
    active = context.view_layer.objects.active
    if active is not None and active.mode != "OBJECT":
        bpy.ops.object.mode_set(mode="OBJECT")


def _resolve_export_armature(context: bpy.types.Context, selection_only: bool) -> Optional[bpy.types.Object]:
    selected_armatures = [obj for obj in context.selected_objects if obj.type == "ARMATURE"]
    if selected_armatures:
        return selected_armatures[0]

    active = context.view_layer.objects.active
    if active and active.type == "ARMATURE":
        return active

    if selection_only:
        return None

    for obj in context.scene.objects:
        if obj.type == "ARMATURE":
            return obj
    return None


def _armature_modifier_targets(obj: bpy.types.Object) -> List[bpy.types.Object]:
    targets: List[bpy.types.Object] = []
    for modifier in obj.modifiers:
        if modifier.type == "ARMATURE" and modifier.object is not None:
            targets.append(modifier.object)
    return targets


def _is_armature_driven_mesh(obj: bpy.types.Object, armature_obj: Optional[bpy.types.Object]) -> bool:
    return armature_obj is not None and armature_obj in _armature_modifier_targets(obj)


def _single_deform_group_name(obj: bpy.types.Object, armature_obj: bpy.types.Object) -> Optional[str]:
    if obj.type != "MESH" or not obj.vertex_groups:
        return None

    deform_names = {bone.name for bone in armature_obj.data.bones if bone.use_deform}
    used_names = set()

    for vertex in obj.data.vertices:
        for element in vertex.groups:
            if element.weight <= 0.0:
                continue
            group_name = obj.vertex_groups[element.group].name
            if group_name in deform_names:
                used_names.add(group_name)
                if len(used_names) > 1:
                    return None
    if len(used_names) == 1:
        return next(iter(used_names))
    return None


def _has_multi_bone_skinning(obj: bpy.types.Object, armature_obj: bpy.types.Object) -> bool:
    if obj.type != "MESH" or not obj.vertex_groups:
        return False

    deform_names = {bone.name for bone in armature_obj.data.bones if bone.use_deform}
    for polygon in obj.data.polygons:
        polygon_bones = set()
        for vertex_index in polygon.vertices:
            vertex = obj.data.vertices[vertex_index]
            for element in vertex.groups:
                if element.weight <= 0.0:
                    continue
                group_name = obj.vertex_groups[element.group].name
                if group_name in deform_names:
                    polygon_bones.add(group_name)
                    if len(polygon_bones) > 1:
                        return True
    return False


def _mesh_belongs_to_armature(obj: bpy.types.Object, armature_obj: bpy.types.Object) -> bool:
    if obj.parent == armature_obj:
        return True
    if obj.parent_type == "BONE" and obj.parent == armature_obj:
        return True
    if armature_obj in _armature_modifier_targets(obj):
        return True
    custom_bone = obj.get("animatruc_bone")
    if isinstance(custom_bone, str) and custom_bone in armature_obj.data.bones:
        return True
    if _single_deform_group_name(obj, armature_obj):
        return True
    return False

def _collect_export_mesh_objects(context: bpy.types.Context, armature_obj: Optional[bpy.types.Object], selection_only: bool) -> List[bpy.types.Object]:
    objects = context.selected_objects if selection_only else context.scene.objects
    meshes: List[bpy.types.Object] = []

    for obj in objects:
        if obj.type != "MESH":
            continue
        if armature_obj and not _mesh_belongs_to_armature(obj, armature_obj):
            continue
        meshes.append(obj)

    return meshes


def _resolve_mesh_bone_name(obj: bpy.types.Object, armature_obj: Optional[bpy.types.Object], summary: ExportSummary) -> Optional[str]:
    if armature_obj is None:
        return "root"

    if obj.parent == armature_obj and obj.parent_type == "BONE" and obj.parent_bone:
        return obj.parent_bone

    custom_bone = obj.get("animatruc_bone")
    if isinstance(custom_bone, str) and custom_bone in armature_obj.data.bones:
        return custom_bone

    single_group = _single_deform_group_name(obj, armature_obj)
    if single_group:
        return single_group

    if _has_multi_bone_skinning(obj, armature_obj):
        return "root"

    return "root"


def _should_export_mesh_skin(obj: bpy.types.Object, armature_obj: Optional[bpy.types.Object]) -> bool:
    if armature_obj is None or obj.type != "MESH":
        return False

    if _has_multi_bone_skinning(obj, armature_obj):
        return True

    if _is_armature_driven_mesh(obj, armature_obj):
        if obj.parent == armature_obj and obj.parent_type == "BONE" and obj.parent_bone:
            return False
        if isinstance(obj.get("animatruc_bone"), str):
            return False
        if _single_deform_group_name(obj, armature_obj):
            return True

    return False


def _copy_mesh_for_export(obj: bpy.types.Object, depsgraph: bpy.types.Depsgraph, apply_modifiers: bool, summary: ExportSummary) -> bpy.types.Mesh:
    has_armature_modifier = any(modifier.type == "ARMATURE" for modifier in obj.modifiers)
    if has_armature_modifier:
        summary.warnings.append(
            f"Mesh '{obj.name}' has an armature modifier. The exporter uses the undeformed rest mesh for AnimaTruc skinning."
        )
        return obj.data.copy()

    if apply_modifiers:
        evaluated = obj.evaluated_get(depsgraph)
        return bpy.data.meshes.new_from_object(
            evaluated,
            preserve_all_data_layers=True,
            depsgraph=depsgraph,
        )

    return obj.data.copy()


def _material_image(material: Optional[bpy.types.Material]) -> Optional[bpy.types.Image]:
    if material is None or not material.use_nodes or material.node_tree is None:
        return None

    for node in material.node_tree.nodes:
        if node.type == "TEX_IMAGE" and getattr(node, "image", None) is not None:
            return node.image
    return None


def _texture_name_from_image(image: bpy.types.Image) -> str:
    source = bpy.path.basename(image.filepath_raw or image.filepath or image.name)
    stem = Path(source).stem or image.name
    return _sanitize_project_name(stem)


def _default_render_type(material: Optional[bpy.types.Material]) -> str:
    if material and material.blend_method in {"BLEND", "HASHED"}:
        return "entityTranslucent"
    return "entityCutoutNoCull"


def _build_pack_render_metadata(mesh_objects: Sequence[bpy.types.Object]) -> Tuple[List[Dict[str, object]], List[Dict[str, object]], Dict[str, str]]:
    textures: List[Dict[str, object]] = []
    materials: List[Dict[str, object]] = []
    object_material_name: Dict[str, str] = {}
    seen_textures: set[str] = set()
    seen_materials: set[str] = set()

    for obj in mesh_objects:
        for slot in obj.material_slots:
            material = slot.material
            if material is None:
                continue

            object_material_name.setdefault(obj.name, material.name)
            image = _material_image(material)
            texture_name: Optional[str] = None

            if image is not None:
                texture_name = _texture_name_from_image(image)
                if texture_name not in seen_textures:
                    seen_textures.add(texture_name)
                    try:
                        texture_path = bpy.path.abspath(image.filepath, library=image.library)
                    except RuntimeError:
                        texture_path = image.filepath or image.filepath_raw or image.name

                    textures.append({
                        "name": texture_name,
                        "path": texture_path,
                        "width": int(image.size[0]) if len(image.size) > 0 else 0,
                        "height": int(image.size[1]) if len(image.size) > 1 else 0,
                    })

            if material.name not in seen_materials:
                seen_materials.add(material.name)
                materials.append({
                    "name": material.name,
                    "texture": texture_name,
                    "renderType": _default_render_type(material),
                })

    return textures, materials, object_material_name


def _armature_space_vertex(obj: bpy.types.Object, armature_obj: bpy.types.Object, coordinate: Vector) -> Vector:
    return armature_obj.matrix_world.inverted_safe() @ (obj.matrix_world @ coordinate)


def _vertex_influences(obj: bpy.types.Object, armature_obj: bpy.types.Object, vertex: bpy.types.MeshVertex) -> List[Dict[str, object]]:
    deform_names = {bone.name for bone in armature_obj.data.bones if bone.use_deform}
    influences: List[Tuple[str, float]] = []

    for element in vertex.groups:
        if element.weight <= 0.0:
            continue
        group_name = obj.vertex_groups[element.group].name
        if group_name in deform_names:
            influences.append((group_name, float(element.weight)))

    if not influences:
        return [{"bone": "root", "weight": 1.0}]

    influences.sort(key=lambda item: item[1], reverse=True)
    trimmed = influences[:4]
    total = sum(weight for _, weight in trimmed)

    if total <= FLOAT_EPSILON:
        return [{"bone": "root", "weight": 1.0}]

    return [{"bone": bone_name, "weight": _float(weight / total)} for bone_name, weight in trimmed]


def _export_mesh_skin(mesh: bpy.types.Mesh, obj: bpy.types.Object, armature_obj: bpy.types.Object) -> Dict[str, object]:
    influences = [_vertex_influences(obj, armature_obj, vertex) for vertex in mesh.vertices]
    return {
        "modelSpaceVertices": True,
        "influences": influences,
    }


def _triangulate_mesh(mesh: bpy.types.Mesh) -> None:
    import bmesh

    bm = bmesh.new()
    bm.from_mesh(mesh)
    bmesh.ops.triangulate(bm, faces=list(bm.faces))
    bm.to_mesh(mesh)
    bm.free()
    mesh.update(calc_edges=True)


def _build_export_skeleton(armature_obj: Optional[bpy.types.Object], pack_unit_scale: float) -> List[Dict[str, object]]:
    if armature_obj is None:
        return [{
            "name": "root",
            "parent": None,
            "pivot": [0.0, 0.0, 0.0],
            "bindPose": _identity_bind_pose(),
            "length": 1.0,
        }]

    bones = _sorted_bones_parent_first(armature_obj.data.bones)
    exported: List[Dict[str, object]] = []

    for bone in bones:
        parent = bone.parent
        local_matrix = (parent.matrix_local.inverted_safe() @ bone.matrix_local) if parent else bone.matrix_local.copy()
        local_rotation = local_matrix.to_quaternion().normalized()
        local_scale = local_matrix.to_scale()

        exported.append({
            "name": bone.name,
            "parent": parent.name if parent else None,
            "pivot": _vec3_list(bone.head_local * pack_unit_scale),
            "bindPose": {
                "translation": [0.0, 0.0, 0.0],
                "rotation": _quat_list(local_rotation),
                "scale": _vec3_list(local_scale),
            },
            "length": _float(bone.length * pack_unit_scale),
        })

    return exported


def _export_mesh_object(
    obj: bpy.types.Object,
    armature_obj: Optional[bpy.types.Object],
    depsgraph: bpy.types.Depsgraph,
    options: ExportOptions,
    summary: ExportSummary,
    material_name_by_object: Dict[str, str],
) -> Optional[Dict[str, object]]:
    export_as_skin = _should_export_mesh_skin(obj, armature_obj)
    bone_name = "root" if export_as_skin else _resolve_mesh_bone_name(obj, armature_obj, summary)
    if not bone_name:
        return None

    mesh = _copy_mesh_for_export(obj, depsgraph, options.apply_modifiers, summary)
    try:
        if options.triangulate_meshes:
            _triangulate_mesh(mesh)

        uv_layer = mesh.uv_layers.active.data if mesh.uv_layers.active else None
        local_location, local_rotation, local_scale = obj.matrix_basis.decompose()
        basis_without_translation = Matrix.LocRotScale(Vector((0.0, 0.0, 0.0)), local_rotation, local_scale)

        if export_as_skin:
            vertices = [
                _vec3_list(_armature_space_vertex(obj, armature_obj, vertex.co) * options.pack_unit_scale)
                for vertex in mesh.vertices
            ]
            origin = [0.0, 0.0, 0.0]
            skin = _export_mesh_skin(mesh, obj, armature_obj)
        else:
            vertices = [_vec3_list((basis_without_translation @ vertex.co) * options.pack_unit_scale) for vertex in mesh.vertices]
            origin = _vec3_list(local_location * options.pack_unit_scale)
            skin = None

        faces: List[Dict[str, object]] = []

        for polygon in mesh.polygons:
            face = {"indices": [int(index) for index in polygon.vertices]}
            if uv_layer:
                face["uvs"] = [
                    [
                        _float(uv_layer[loop_index].uv.x),
                        _float(uv_layer[loop_index].uv.y),
                    ]
                    for loop_index in polygon.loop_indices
                ]
            faces.append(face)

        if not vertices or not faces:
            summary.warnings.append(f"Mesh '{obj.name}' produced no geometry and was skipped.")
            return None

        summary.meshes_exported += 1
        exported = {
            "name": obj.name,
            "bone": bone_name,
            "origin": origin,
            "vertices": vertices,
            "faces": faces,
            "sourceObject": obj.name,
        }
        material_name = material_name_by_object.get(obj.name)
        if material_name:
            exported["material"] = material_name
        if skin is not None:
            exported["skin"] = skin
        return exported
    finally:
        bpy.data.meshes.remove(mesh)


def _actions_for_armature(armature_obj: bpy.types.Object) -> List[bpy.types.Action]:
    bone_names = {bone.name for bone in armature_obj.data.bones}
    actions: List[bpy.types.Action] = []
    for action in bpy.data.actions:
        if any((_extract_bone_name(fcurve.data_path) in bone_names) for fcurve in _action_fcurves(action)):
            actions.append(action)
    return actions


def _action_fcurves(action: bpy.types.Action) -> List[bpy.types.FCurve]:
    fcurves: List[bpy.types.FCurve] = []

    if hasattr(action, "fcurves"):
        return list(action.fcurves)

    for layer in getattr(action, "layers", []):
        for strip in getattr(layer, "strips", []):
            for channelbag in getattr(strip, "channelbags", []):
                fcurves.extend(list(getattr(channelbag, "fcurves", [])))

    return fcurves


def _channel_curves(action: bpy.types.Action, bone_name: str, channel: str) -> List[bpy.types.FCurve]:
    curves: List[bpy.types.FCurve] = []
    for fcurve in _action_fcurves(action):
        if _extract_bone_name(fcurve.data_path) != bone_name:
            continue
        if _channel_from_data_path(fcurve.data_path) == channel:
            curves.append(fcurve)
    return curves


def _interpolation_for_frame(curves: List[bpy.types.FCurve], frame: float) -> str:
    for fcurve in curves:
        for keyframe in fcurve.keyframe_points:
            if abs(keyframe.co.x - frame) <= 1.0e-4 and keyframe.interpolation == "CONSTANT":
                return "STEP"
    return "LINEAR"


def _channel_is_default(channel: str, values: List[Sequence[float]]) -> bool:
    if not values:
        return True

    if channel == "translation":
        return all(all(abs(component) <= FLOAT_EPSILON for component in value) for value in values)
    if channel == "rotation":
        return all(
            abs(value[0]) <= FLOAT_EPSILON
            and abs(value[1]) <= FLOAT_EPSILON
            and abs(value[2]) <= FLOAT_EPSILON
            and abs(value[3] - 1.0) <= FLOAT_EPSILON
            for value in values
        )
    if channel == "scale":
        return all(all(abs(component - 1.0) <= FLOAT_EPSILON for component in value) for value in values)
    return True


def _build_export_clips(
    context: bpy.types.Context,
    armature_obj: Optional[bpy.types.Object],
    options: ExportOptions,
    summary: ExportSummary,
) -> List[Dict[str, object]]:
    if armature_obj is None or not options.export_actions:
        return []

    actions = _actions_for_armature(armature_obj)
    if not actions:
        return []

    scene = context.scene
    fps = _effective_fps(scene)
    animation_data = armature_obj.animation_data_create()
    original_action = animation_data.action
    original_frame = scene.frame_current_final
    clips: List[Dict[str, object]] = []

    try:
        for action in actions:
            bone_channels: Dict[str, set[str]] = {}
            frames = set()

            for fcurve in _action_fcurves(action):
                bone_name = _extract_bone_name(fcurve.data_path)
                channel = _channel_from_data_path(fcurve.data_path)
                if bone_name is None or channel is None:
                    continue
                bone_channels.setdefault(bone_name, set()).add(channel)
                for keyframe in fcurve.keyframe_points:
                    frames.add(float(keyframe.co.x))

            if not bone_channels or not frames:
                continue

            ordered_frames = sorted(frames)
            start_frame = ordered_frames[0]
            animation_data.action = action
            context.view_layer.update()
            tracks: Dict[str, Dict[str, List[Dict[str, object]]]] = {}

            for frame in ordered_frames:
                whole = math.floor(frame)
                scene.frame_set(whole, subframe=frame - whole)
                context.view_layer.update()
                tick = max(0.0, (frame - start_frame) * options.ticks_per_second / fps)

                for bone_name, channels in bone_channels.items():
                    pose_bone = armature_obj.pose.bones.get(bone_name)
                    if pose_bone is None:
                        continue

                    location, rotation, scale = pose_bone.matrix_basis.decompose()
                    track = tracks.setdefault(bone_name, {"translation": [], "rotation": [], "scale": []})

                    if "translation" in channels:
                        track["translation"].append({
                            "tick": _float(tick),
                            "interpolation": _interpolation_for_frame(_channel_curves(action, bone_name, "translation"), frame),
                            "value": _vec3_list(location * options.pack_unit_scale),
                        })

                    if "rotation" in channels:
                        track["rotation"].append({
                            "tick": _float(tick),
                            "interpolation": _interpolation_for_frame(_channel_curves(action, bone_name, "rotation"), frame),
                            "value": _quat_list(rotation),
                        })

                    if "scale" in channels:
                        track["scale"].append({
                            "tick": _float(tick),
                            "interpolation": _interpolation_for_frame(_channel_curves(action, bone_name, "scale"), frame),
                            "value": _vec3_list(scale),
                        })

            filtered_tracks: Dict[str, Dict[str, List[Dict[str, object]]]] = {}
            for bone_name, track in tracks.items():
                kept_track: Dict[str, List[Dict[str, object]]] = {}
                for channel_name in ("translation", "rotation", "scale"):
                    channel_values = [entry["value"] for entry in track[channel_name]]
                    if track[channel_name] and (not _channel_is_default(channel_name, channel_values) or len(track[channel_name]) > 1):
                        kept_track[channel_name] = track[channel_name]
                if kept_track:
                    filtered_tracks[bone_name] = kept_track

            if not filtered_tracks:
                continue

            clip = {
                "name": action.name,
                "lengthTicks": _float((ordered_frames[-1] - start_frame) * options.ticks_per_second / fps),
                "looping": True,
                "tracks": filtered_tracks,
            }
            clips.append(clip)
            summary.clips_exported += 1
    finally:
        animation_data.action = original_action
        whole = math.floor(original_frame)
        scene.frame_set(whole, subframe=original_frame - whole)
        context.view_layer.update()

    return clips


def build_animatruc_pack(context: bpy.types.Context, options: ExportOptions) -> Tuple[Dict[str, object], ExportSummary]:
    _ensure_object_mode(context)
    summary = ExportSummary()
    armature_obj = _resolve_export_armature(context, options.selection_only)
    depsgraph = context.evaluated_depsgraph_get()
    mesh_objects = _collect_export_mesh_objects(context, armature_obj, options.selection_only)
    textures, materials, material_name_by_object = _build_pack_render_metadata(mesh_objects)

    project_name = Path(bpy.data.filepath).stem if bpy.data.filepath else context.scene.name
    pack = {
        "format": "animatruc-pack",
        "version": PACK_VERSION,
        "meta": {
            "source": "blender",
            "pluginId": ADDON_ID,
            "projectName": _sanitize_project_name(project_name),
            "sourceVersion": bpy.app.version_string,
            "exportedAt": datetime.now(timezone.utc).isoformat(),
            "textures": textures,
            "materials": materials,
        },
        "skeleton": {
            "bones": _build_export_skeleton(armature_obj, options.pack_unit_scale),
        },
        "model": {
            "cubes": [],
            "meshes": [],
        },
        "clips": [],
    }

    for mesh_object in mesh_objects:
        exported = _export_mesh_object(mesh_object, armature_obj, depsgraph, options, summary, material_name_by_object)
        if exported is not None:
            pack["model"]["meshes"].append(exported)

    pack["clips"] = _build_export_clips(context, armature_obj, options, summary)

    if not pack["model"]["meshes"]:
        summary.warnings.append(
            "No mesh geometry was exported. Use mesh objects parented to bones/root or skinned to the selected armature."
        )

    return pack, summary


def write_animatruc_pack(context: bpy.types.Context, options: ExportOptions) -> ExportSummary:
    pack, summary = build_animatruc_pack(context, options)
    canonical_path = _canonical_animatruc_json_filepath(options.filepath)
    Path(canonical_path).parent.mkdir(parents=True, exist_ok=True)
    with open(canonical_path, "w", encoding="utf-8") as handle:
        json.dump(pack, handle, indent=2, ensure_ascii=False)
    return summary


def _parse_bone_specs(bones_payload: Sequence[dict], pack_unit_scale: float) -> Dict[str, BoneSpec]:
    specs: Dict[str, BoneSpec] = {}
    safe_scale = pack_unit_scale if abs(pack_unit_scale) > FLOAT_EPSILON else 1.0

    for bone in bones_payload:
        name = str(bone.get("name", "")).strip()
        if not name:
            continue

        bind_pose = bone.get("bindPose") or {}
        pivot = _vec3(bone.get("pivot", (0.0, 0.0, 0.0))) / safe_scale
        local_rotation = _quat_from_pack(bind_pose.get("rotation", (0.0, 0.0, 0.0, 1.0)))
        local_scale = _vec3(bind_pose.get("scale", (1.0, 1.0, 1.0)))
        length = float(bone.get("length", DEFAULT_BONE_LENGTH * safe_scale)) / safe_scale
        specs[name] = BoneSpec(
            name=name,
            parent=bone.get("parent"),
            pivot=pivot,
            local_rotation=local_rotation,
            local_scale=local_scale,
            length=max(length, DEFAULT_BONE_LENGTH * 0.1),
        )

    return specs


def _order_bone_specs(specs: Dict[str, BoneSpec]) -> List[BoneSpec]:
    remaining = dict(specs)
    ordered: List[BoneSpec] = []

    while remaining:
        progressed = False
        for name, spec in list(remaining.items()):
            if spec.parent is None or spec.parent not in remaining:
                ordered.append(spec)
                remaining.pop(name)
                progressed = True
        if not progressed:
            ordered.extend(remaining.values())
            break

    return ordered


def _children_by_parent(specs: Dict[str, BoneSpec]) -> Dict[str, List[BoneSpec]]:
    children: Dict[str, List[BoneSpec]] = {}
    for spec in specs.values():
        if spec.parent:
            children.setdefault(spec.parent, []).append(spec)
    return children


def _absolute_rotation_cache(specs: Dict[str, BoneSpec]) -> Dict[str, Quaternion]:
    cache: Dict[str, Quaternion] = {}

    def resolve(name: str) -> Quaternion:
        if name in cache:
            return cache[name]
        spec = specs[name]
        if spec.parent and spec.parent in specs:
            cache[name] = (resolve(spec.parent) @ spec.local_rotation).normalized()
        else:
            cache[name] = spec.local_rotation.normalized()
        return cache[name]

    for name in specs:
        resolve(name)
    return cache


def _resolve_import_bone_length(spec: BoneSpec, children: List[BoneSpec], parent_spec: Optional[BoneSpec]) -> float:
    if spec.length > FLOAT_EPSILON:
        return spec.length

    if children:
        distances = [(_child.pivot - spec.pivot).length for _child in children if (_child.pivot - spec.pivot).length > FLOAT_EPSILON]
        if distances:
            return min(distances)

    if parent_spec is not None and parent_spec.length > FLOAT_EPSILON:
        return max(parent_spec.length * 0.65, DEFAULT_BONE_LENGTH)

    return DEFAULT_BONE_LENGTH


def _create_armature_from_pack(
    context: bpy.types.Context,
    collection: bpy.types.Collection,
    bones_payload: Sequence[dict],
    pack_unit_scale: float,
    summary: ImportSummary,
) -> Optional[bpy.types.Object]:
    specs = _parse_bone_specs(bones_payload, pack_unit_scale)
    if not specs:
        summary.warnings.append("The pack contains no bones. Meshes will be imported without an armature.")
        return None

    armature_name = _unique_collection_name("AnimaTrucRig")
    armature_data = bpy.data.armatures.new(armature_name)
    armature_object = bpy.data.objects.new(armature_name, armature_data)
    collection.objects.link(armature_object)

    context.view_layer.objects.active = armature_object
    armature_object.select_set(True)
    bpy.ops.object.mode_set(mode="EDIT")

    edit_bones = armature_data.edit_bones
    ordered_specs = _order_bone_specs(specs)
    children_map = _children_by_parent(specs)
    absolute_rotations = _absolute_rotation_cache(specs)

    for spec in ordered_specs:
        edit_bone = edit_bones.new(spec.name)
        parent_spec = specs.get(spec.parent) if spec.parent else None
        length = max(_resolve_import_bone_length(spec, children_map.get(spec.name, []), parent_spec), DEFAULT_BONE_LENGTH * 0.1)
        absolute_rotation = absolute_rotations[spec.name]
        direction = absolute_rotation @ Vector((0.0, 1.0, 0.0))
        if direction.length <= FLOAT_EPSILON:
            direction = Vector((0.0, 1.0, 0.0))

        child_specs = children_map.get(spec.name, [])
        if child_specs:
            child_vector = Vector((0.0, 0.0, 0.0))
            for child_spec in child_specs:
                child_vector += (child_spec.pivot - spec.pivot)
            if child_vector.length > FLOAT_EPSILON and direction.dot(child_vector.normalized()) < 0.0:
                direction.negate()

        edit_bone.head = spec.pivot
        edit_bone.tail = spec.pivot + direction.normalized() * length
        if spec.parent and spec.parent in edit_bones:
            edit_bone.parent = edit_bones[spec.parent]
            edit_bone.use_connect = False

        roll_axis = absolute_rotation @ Vector((0.0, 0.0, 1.0))
        if roll_axis.length > FLOAT_EPSILON:
            try:
                edit_bone.align_roll(roll_axis.normalized())
            except RuntimeError:
                pass

    bpy.ops.object.mode_set(mode="OBJECT")
    return armature_object


def _apply_uvs(mesh: bpy.types.Mesh, faces_payload: Sequence[dict]) -> None:
    uv_needed = any(face.get("uvs") for face in faces_payload)
    if not uv_needed:
        return

    uv_layer = mesh.uv_layers.new(name="UVMap")
    uv_data = uv_layer.data
    for polygon, face in zip(mesh.polygons, faces_payload):
        face_uvs = face.get("uvs") or []
        for offset, loop_index in enumerate(polygon.loop_indices):
            if offset < len(face_uvs) and len(face_uvs[offset]) >= 2:
                uv_data[loop_index].uv = (float(face_uvs[offset][0]), float(face_uvs[offset][1]))


def _resolve_import_texture_path(pack_path: str, texture_path: str) -> Optional[Path]:
    if not texture_path:
        return None

    candidate = Path(texture_path)
    if candidate.is_file():
        return candidate

    relative_candidate = (Path(pack_path).parent / texture_path).resolve()
    if relative_candidate.is_file():
        return relative_candidate

    return None


def _build_import_material_library(pack_path: str, payload: dict) -> Dict[str, bpy.types.Material]:
    metadata = payload.get("meta") or {}
    textures_payload = metadata.get("textures") or []
    materials_payload = metadata.get("materials") or []
    textures_by_name = {str(texture.get("name")): texture for texture in textures_payload if texture.get("name")}
    material_library: Dict[str, bpy.types.Material] = {}

    for material_payload in materials_payload:
        material_name = str(material_payload.get("name") or "").strip()
        if not material_name:
            continue

        material = bpy.data.materials.get(material_name) or bpy.data.materials.new(name=material_name)
        material.use_nodes = True
        material_library[material_name] = material
        node_tree = material.node_tree
        if node_tree is None:
            continue

        principled = next((node for node in node_tree.nodes if node.type == "BSDF_PRINCIPLED"), None)
        output = next((node for node in node_tree.nodes if node.type == "OUTPUT_MATERIAL"), None)
        if principled is None or output is None:
            continue

        texture_name = material_payload.get("texture")
        texture_payload = textures_by_name.get(texture_name) if texture_name else None
        texture_path = _resolve_import_texture_path(pack_path, str(texture_payload.get("path") or "")) if texture_payload else None
        if texture_path is None:
            continue

        image = bpy.data.images.load(str(texture_path), check_existing=True)
        image_node = next((node for node in node_tree.nodes if node.type == "TEX_IMAGE"), None)
        if image_node is None:
            image_node = node_tree.nodes.new(type="ShaderNodeTexImage")
            image_node.location = principled.location + Vector((-320.0, 0.0))

        image_node.image = image
        if not any(link.from_node == image_node and link.to_node == principled for link in node_tree.links):
            node_tree.links.new(image_node.outputs["Color"], principled.inputs["Base Color"])

    return material_library


def _assign_material(mesh: bpy.types.Mesh, material_name: Optional[str], material_library: Dict[str, bpy.types.Material]) -> None:
    if not material_name:
        return

    material = material_library.get(material_name)
    if material is None:
        material = bpy.data.materials.get(material_name) or bpy.data.materials.new(name=material_name)
        material_library[material_name] = material

    if not any(existing_material == material for existing_material in mesh.materials):
        mesh.materials.append(material)


def _apply_skin_weights(
    mesh_object: bpy.types.Object,
    armature_object: bpy.types.Object,
    influences_payload: Sequence[Sequence[dict]],
) -> None:
    mesh_object.parent = armature_object
    modifier = mesh_object.modifiers.new(name="AnimaTrucArmature", type="ARMATURE")
    modifier.object = armature_object

    groups: Dict[str, bpy.types.VertexGroup] = {}
    for influences in influences_payload:
        for influence in influences:
            bone_name = str(influence.get("bone") or "").strip()
            if bone_name and bone_name not in groups:
                groups[bone_name] = mesh_object.vertex_groups.new(name=bone_name)

    for vertex_index, influences in enumerate(influences_payload):
        for influence in influences:
            bone_name = str(influence.get("bone") or "").strip()
            weight = float(influence.get("weight") or 0.0)
            if not bone_name or weight <= 0.0:
                continue
            groups[bone_name].add([vertex_index], weight, "REPLACE")


def _create_mesh_object_from_pack(
    collection: bpy.types.Collection,
    armature_object: Optional[bpy.types.Object],
    mesh_payload: dict,
    pack_unit_scale: float,
    summary: ImportSummary,
    material_library: Dict[str, bpy.types.Material],
) -> Optional[bpy.types.Object]:
    name = str(mesh_payload.get("name") or mesh_payload.get("bone") or "animatruc_mesh")
    safe_scale = pack_unit_scale if abs(pack_unit_scale) > FLOAT_EPSILON else 1.0
    vertices = [_vec3(vertex) / safe_scale for vertex in (mesh_payload.get("vertices") or [])]
    faces_payload = mesh_payload.get("faces") or []
    faces = [list(map(int, face.get("indices") or [])) for face in faces_payload if len(face.get("indices") or []) >= 3]

    if not vertices or not faces:
        summary.warnings.append(f"Mesh '{name}' has no usable vertices or faces and was skipped on import.")
        return None

    mesh = bpy.data.meshes.new(name)
    mesh.from_pydata(vertices, [], faces)
    mesh.validate(clean_customdata=False)
    mesh.update(calc_edges=True)
    _apply_uvs(mesh, faces_payload)
    _assign_material(mesh, mesh_payload.get("material"), material_library)

    mesh_object = bpy.data.objects.new(name, mesh)
    collection.objects.link(mesh_object)

    origin = _vec3(mesh_payload.get("origin", (0.0, 0.0, 0.0))) / safe_scale
    bone_name = mesh_payload.get("bone")
    skin_payload = mesh_payload.get("skin") or {}
    influences_payload = skin_payload.get("influences") or []
    model_space_vertices = bool(skin_payload.get("modelSpaceVertices", False))

    if armature_object and influences_payload:
        mesh_object.location = Vector((0.0, 0.0, 0.0)) if model_space_vertices else origin
        _apply_skin_weights(mesh_object, armature_object, influences_payload)
    elif armature_object and isinstance(bone_name, str) and bone_name in armature_object.data.bones:
        mesh_object.parent = armature_object
        mesh_object.parent_type = "BONE"
        mesh_object.parent_bone = bone_name
        mesh_object.location = origin
        mesh_object["animatruc_bone"] = bone_name
    else:
        mesh_object.location = origin

    summary.meshes_imported += 1
    return mesh_object


def _cube_faces() -> List[List[int]]:
    return [
        [0, 1, 3, 2],
        [4, 6, 7, 5],
        [0, 2, 6, 4],
        [1, 5, 7, 3],
        [0, 4, 5, 1],
        [2, 3, 7, 6],
    ]


def _create_cube_object_from_pack(
    collection: bpy.types.Collection,
    armature_object: Optional[bpy.types.Object],
    cube_payload: dict,
    pack_unit_scale: float,
    summary: ImportSummary,
    material_library: Dict[str, bpy.types.Material],
) -> Optional[bpy.types.Object]:
    name = str(cube_payload.get("name") or cube_payload.get("bone") or "animatruc_cube")
    safe_scale = pack_unit_scale if abs(pack_unit_scale) > FLOAT_EPSILON else 1.0
    from_vec = _vec3(cube_payload.get("from", (0.0, 0.0, 0.0))) / safe_scale
    to_vec = _vec3(cube_payload.get("to", (0.0, 0.0, 0.0))) / safe_scale
    center = (from_vec + to_vec) * 0.5
    min_vec = from_vec - center
    max_vec = to_vec - center

    vertices = [
        Vector((min_vec.x, min_vec.y, min_vec.z)),
        Vector((max_vec.x, min_vec.y, min_vec.z)),
        Vector((min_vec.x, max_vec.y, min_vec.z)),
        Vector((max_vec.x, max_vec.y, min_vec.z)),
        Vector((min_vec.x, min_vec.y, max_vec.z)),
        Vector((max_vec.x, min_vec.y, max_vec.z)),
        Vector((min_vec.x, max_vec.y, max_vec.z)),
        Vector((max_vec.x, max_vec.y, max_vec.z)),
    ]

    mesh = bpy.data.meshes.new(name)
    mesh.from_pydata(vertices, [], _cube_faces())
    mesh.validate(clean_customdata=False)
    mesh.update(calc_edges=True)
    _assign_material(mesh, cube_payload.get("material"), material_library)

    cube_object = bpy.data.objects.new(name, mesh)
    collection.objects.link(cube_object)

    bone_name = cube_payload.get("bone")
    if armature_object and isinstance(bone_name, str) and bone_name in armature_object.data.bones:
        cube_object.parent = armature_object
        cube_object.parent_type = "BONE"
        cube_object.parent_bone = bone_name
        cube_object.location = center
        cube_object["animatruc_bone"] = bone_name
    else:
        cube_object.location = center

    summary.cubes_imported += 1
    return cube_object


def _set_action_keyframe_interpolations(action: bpy.types.Action, interpolation_map: Dict[Tuple[str, int, float], str]) -> None:
    for fcurve in _action_fcurves(action):
        for keyframe in fcurve.keyframe_points:
            key = (fcurve.data_path, fcurve.array_index, round(float(keyframe.co.x), 6))
            interpolation = interpolation_map.get(key)
            if interpolation == "STEP":
                keyframe.interpolation = "CONSTANT"
            elif interpolation == "LINEAR":
                keyframe.interpolation = "LINEAR"


def _create_actions_from_pack(
    armature_object: Optional[bpy.types.Object],
    clips_payload: Sequence[dict],
    ticks_per_second: int,
    summary: ImportSummary,
) -> None:
    if armature_object is None:
        if clips_payload:
            summary.warnings.append("Animation clips were found but no armature could be created, so clips were skipped.")
        return

    fps = _effective_fps(bpy.context.scene)
    animation_data = armature_object.animation_data_create()

    for pose_bone in armature_object.pose.bones:
        pose_bone.rotation_mode = "QUATERNION"

    for clip in clips_payload:
        clip_name = str(clip.get("name") or f"clip_{summary.clips_imported}")
        action = bpy.data.actions.new(name=clip_name)
        action.use_fake_user = True
        animation_data.action = action
        interpolation_map: Dict[Tuple[str, int, float], str] = {}

        for bone_name, track in (clip.get("tracks") or {}).items():
            pose_bone = armature_object.pose.bones.get(bone_name)
            if pose_bone is None:
                summary.warnings.append(f"Clip '{clip_name}' references bone '{bone_name}' which is missing in the imported armature.")
                continue

            pose_bone.rotation_mode = "QUATERNION"

            for entry in track.get("translation", []):
                frame = float(entry.get("tick", 0.0)) * fps / max(float(ticks_per_second), 1.0)
                value = _vec3(entry.get("value", (0.0, 0.0, 0.0)))
                pose_bone.location = value
                pose_bone.keyframe_insert(data_path="location", frame=frame, group=bone_name)
                interpolation = str(entry.get("interpolation", "LINEAR")).upper()
                for index in range(3):
                    interpolation_map[(f'pose.bones["{bone_name}"].location', index, round(frame, 6))] = interpolation

            for entry in track.get("rotation", []):
                frame = float(entry.get("tick", 0.0)) * fps / max(float(ticks_per_second), 1.0)
                quaternion = _quat_from_pack(entry.get("value", (0.0, 0.0, 0.0, 1.0)))
                pose_bone.rotation_quaternion = quaternion
                pose_bone.keyframe_insert(data_path="rotation_quaternion", frame=frame, group=bone_name)
                interpolation = str(entry.get("interpolation", "LINEAR")).upper()
                for index in range(4):
                    interpolation_map[(f'pose.bones["{bone_name}"].rotation_quaternion', index, round(frame, 6))] = interpolation

            for entry in track.get("scale", []):
                frame = float(entry.get("tick", 0.0)) * fps / max(float(ticks_per_second), 1.0)
                value = _vec3(entry.get("value", (1.0, 1.0, 1.0)))
                pose_bone.scale = value
                pose_bone.keyframe_insert(data_path="scale", frame=frame, group=bone_name)
                interpolation = str(entry.get("interpolation", "LINEAR")).upper()
                for index in range(3):
                    interpolation_map[(f'pose.bones["{bone_name}"].scale', index, round(frame, 6))] = interpolation

        _set_action_keyframe_interpolations(action, interpolation_map)
        summary.clips_imported += 1


def import_animatruc_pack(context: bpy.types.Context, options: ImportOptions) -> ImportSummary:
    _ensure_object_mode(context)
    summary = ImportSummary()

    with open(options.filepath, "r", encoding="utf-8") as handle:
        payload = json.load(handle)
    material_library = _build_import_material_library(options.filepath, payload)

    meta = payload.get("meta") or {}
    project_name = _sanitize_project_name(str(meta.get("projectName") or Path(options.filepath).stem))
    collection: bpy.types.Collection
    if options.create_collection:
        collection_name = _unique_collection_name(f"AnimaTruc_{project_name}")
        collection = bpy.data.collections.new(collection_name)
        context.scene.collection.children.link(collection)
    else:
        collection = context.collection

    armature_object = _create_armature_from_pack(
        context,
        collection,
        (payload.get("skeleton") or {}).get("bones") or [],
        options.pack_unit_scale,
        summary,
    )

    for cube_payload in (payload.get("model") or {}).get("cubes") or []:
        _create_cube_object_from_pack(collection, armature_object, cube_payload, options.pack_unit_scale, summary, material_library)

    for mesh_payload in (payload.get("model") or {}).get("meshes") or []:
        _create_mesh_object_from_pack(collection, armature_object, mesh_payload, options.pack_unit_scale, summary, material_library)

    if options.create_actions:
        _create_actions_from_pack(armature_object, payload.get("clips") or [], options.ticks_per_second, summary)

    return summary


class ANIMATRUC_OT_export_pack(Operator, ExportHelper):
    bl_idname = "export_scene.animatruc_pack"
    bl_label = "Exporter AnimaTruc Pack"
    bl_options = {"REGISTER", "UNDO"}

    filename_ext = ".animatruc.json"
    filter_glob: StringProperty(default="*.animatrucpack;*.animatrucpack.json;*.animatruc.json;*.json", options={"HIDDEN"})
    pack_unit_scale: FloatProperty(name="Pack Unit Scale", description="Multiplier applied to Blender coordinates before writing the pack", default=1.0, min=0.0001)
    ticks_per_second: IntProperty(name="Ticks Per Second", default=DEFAULT_TICKS_PER_SECOND, min=1, max=240)
    selection_only: BoolProperty(name="Selection Only", description="Export only selected mesh objects and the selected armature", default=False)
    apply_modifiers: BoolProperty(name="Apply Modifiers", description="Apply non-armature modifiers before export", default=True)
    triangulate_meshes: BoolProperty(name="Triangulate Meshes", description="Triangulate exported polygons", default=False)
    export_actions: BoolProperty(name="Export Actions", description="Export Blender actions as AnimaTruc clips", default=True)

    def check(self, _context: bpy.types.Context):
        canonical = _canonical_animatruc_json_filepath(self.filepath)
        if canonical != self.filepath:
            self.filepath = canonical
            return True
        return False

    def execute(self, context: bpy.types.Context):
        self.filepath = _canonical_animatruc_json_filepath(self.filepath)
        options = ExportOptions(
            filepath=self.filepath,
            pack_unit_scale=self.pack_unit_scale,
            ticks_per_second=self.ticks_per_second,
            selection_only=self.selection_only,
            apply_modifiers=self.apply_modifiers,
            triangulate_meshes=self.triangulate_meshes,
            export_actions=self.export_actions,
        )
        summary = write_animatruc_pack(context, options)
        if summary.warnings:
            self.report({"WARNING"}, " | ".join(summary.warnings[:6]))
        self.report({"INFO"}, f"AnimaTruc export complete: {summary.meshes_exported} mesh(es), {summary.clips_exported} clip(s).")
        return {"FINISHED"}


class ANIMATRUC_OT_import_pack(Operator, ImportHelper):
    bl_idname = "import_scene.animatruc_pack"
    bl_label = "Importer AnimaTruc Pack"
    bl_options = {"REGISTER", "UNDO"}

    filename_ext = ".animatruc.json"
    filter_glob: StringProperty(default="*.animatrucpack;*.animatrucpack.json;*.animatruc.json;*.json", options={"HIDDEN"})
    pack_unit_scale: FloatProperty(name="Pack Unit Scale", description="Divisor applied to pack coordinates before creating Blender objects", default=1.0, min=0.0001)
    ticks_per_second: IntProperty(name="Ticks Per Second", default=DEFAULT_TICKS_PER_SECOND, min=1, max=240)
    create_actions: BoolProperty(name="Create Actions", description="Create Blender actions from clip tracks", default=True)
    create_collection: BoolProperty(name="Create Collection", description="Create a dedicated collection for imported content", default=True)

    def execute(self, context: bpy.types.Context):
        options = ImportOptions(
            filepath=self.filepath,
            pack_unit_scale=self.pack_unit_scale,
            ticks_per_second=self.ticks_per_second,
            create_actions=self.create_actions,
            create_collection=self.create_collection,
        )
        summary = import_animatruc_pack(context, options)
        if summary.warnings:
            self.report({"WARNING"}, " | ".join(summary.warnings[:6]))
        self.report(
            {"INFO"},
            f"AnimaTruc import complete: {summary.meshes_imported} mesh(es), {summary.cubes_imported} cube(s), {summary.clips_imported} clip(s).",
        )
        return {"FINISHED"}


class ANIMATRUC_PT_sidebar(Panel):
    bl_label = "AnimaTruc"
    bl_idname = "ANIMATRUC_PT_sidebar"
    bl_space_type = "VIEW_3D"
    bl_region_type = "UI"
    bl_category = "AnimaTruc"

    def draw(self, context: bpy.types.Context):
        layout = self.layout
        layout.label(text="Runtime Pack I/O")
        layout.operator(ANIMATRUC_OT_import_pack.bl_idname, icon="IMPORT")
        layout.operator(ANIMATRUC_OT_export_pack.bl_idname, icon="EXPORT")
        layout.separator()
        layout.label(text="Forge-ready armature workflow")
        layout.label(text="Rigid parts or multi-bone skinning")


CLASSES = (
    ANIMATRUC_OT_export_pack,
    ANIMATRUC_OT_import_pack,
    ANIMATRUC_PT_sidebar,
)


def _menu_import(self, _context):
    self.layout.operator(ANIMATRUC_OT_import_pack.bl_idname, text="AnimaTruc Pack (.animatruc.json)")


def _menu_export(self, _context):
    self.layout.operator(ANIMATRUC_OT_export_pack.bl_idname, text="AnimaTruc Pack (.animatruc.json)")


def register():
    for cls in CLASSES:
        bpy.utils.register_class(cls)
    bpy.types.TOPBAR_MT_file_import.append(_menu_import)
    bpy.types.TOPBAR_MT_file_export.append(_menu_export)


def unregister():
    bpy.types.TOPBAR_MT_file_import.remove(_menu_import)
    bpy.types.TOPBAR_MT_file_export.remove(_menu_export)
    for cls in reversed(CLASSES):
        bpy.utils.unregister_class(cls)


if __name__ == "__main__":
    register()
