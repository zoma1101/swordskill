/*
 * SwordSkill timeline authoring tools for Blockbench's GeckoLib/Bedrock animation timeline.
 * Skill metadata and gameplay events are stored as timeline-channel custom instructions.
 */
const SWORDSKILL_META_PREFIX = '// swordskill_meta:';
const SWORDSKILL_EVENT_PREFIX = '// swordskill_event:';
const SWORDSKILL_MOLANG_MARKER_SENTINEL = 1048575;
const SWORDSKILL_MOLANG_MARKER_PREFIX = `0*(${SWORDSKILL_MOLANG_MARKER_SENTINEL}+`;
const SWORDSKILL_FIRST_CUSTOM_ID = 85;
const SWORDSKILL_WEAPONS = [
    'ONE_HANDED_SWORD', 'TWO_HANDED_SWORD', 'KATANA', 'AXE', 'RAPIER', 'CLAW',
    'SPEAR', 'MACE', 'SCYTHE', 'DAGGER', 'DUALSWORD'
];
const SWORDSKILL_PREVIEW_BLOCK_SIZE = 16;
const SWORDSKILL_TIMELINE_MARKER_PREFIX = '[SwordSkill Events] ';
const SWORDSKILL_EVENT_MARKER_LABELS = {
    attack: 'Hitbox',
    motion: 'Motion',
    projectile: 'Flying Slash',
    sound: 'Sound'
};

let previewRoot = null;
let previewBoxRoot = null;
let previewBoxes = [];
let previewSignature = '';
let previewBoxMaterial = null;
let previewEdgeMaterial = null;
let previewHandleMaterials = [];
let previewRotationMaterials = [];
let previewListener = null;
let previewAnimationListener = null;
let previewUndoListener = null;
let timelineMarkerSignature = '';
let previewCanvas = null;
let previewCamera = null;
let activeResize = null;
let previewPointerDownListener = null;
let previewPointerMoveListener = null;
let previewPointerUpListener = null;
let previewEditListener = null;
let previewGizmoToolbar = null;
let previewGizmoMode = 'resize';
let previewCanvasParentPosition = null;
let previewGizmoHost = null;

Plugin.register('swordskill_skill_creator', {
    title: 'SwordSkill Timeline Events',
    author: 'SwordSkill',
    description: 'Author skill hits, movement, and contact-triggered RUSH combos on an animation timeline.',
    icon: 'sports_martial_arts',
    version: '2.18.0',
    variant: 'both',
    onload() {
        migrateLegacyMarkers();
        initializeSkillPreview();
        this.actions = [
            new Action('swordskill_setup_metadata', {
                name: 'SwordSkill: Set Skill Metadata',
                description: 'Set skill metadata. The skill name is generated from the animation name.',
                icon: 'tune',
                click: setupSkillMetadata
            }),
            new Action('swordskill_add_attack_event', {
                name: 'SwordSkill: Add Attack at Playhead',
                description: 'Place an attack/hitbox event at the current animation time.',
                icon: 'my_location',
                click: () => openTimelineEventDialog('attack')
            }),
            new Action('swordskill_add_projectile_event', {
                name: 'SwordSkill: Add Flying Slash at Playhead',
                description: 'Spawn a moving slash effect with its own damage and lifetime.',
                icon: 'send',
                click: () => openTimelineEventDialog('projectile')
            }),
            new Action('swordskill_add_motion_event', {
                name: 'SwordSkill: Add Motion at Playhead',
                description: 'Place a user-movement impulse at the current animation time.',
                icon: 'open_with',
                click: () => openTimelineEventDialog('motion')
            }),
            new Action('swordskill_add_sound_event', {
                name: 'SwordSkill: Add Sound at Playhead',
                description: 'Play a sound at the current animation time without an attack hitbox.',
                icon: 'volume_up',
                click: () => openTimelineEventDialog('sound')
            }),
            new Action('swordskill_edit_event', {
                name: 'SwordSkill: Edit Selected Event',
                description: 'Edit the selected SwordSkill event on the timeline.',
                icon: 'edit',
                click: editSelectedTimelineEvent
            }),
            new Action('swordskill_export_data', {
                name: 'SwordSkill: Export skilldata JSON',
                description: 'Generate skilldata JSON from the selected animation and its timeline events.',
                icon: 'file_download',
                click: exportSkillData
            }),
            new Action('swordskill_import_data', {
                name: 'SwordSkill: Import skilldata JSON',
                description: 'Load skilldata JSON into the selected animation timeline for editing.',
                icon: 'file_upload',
                click: importSkillData
            })
        ];
        this.actions.forEach(action => MenuBar.addAction(action, 'tools'));
    },
    onunload() {
        this.actions?.forEach(action => action.delete());
        Blockbench.removeListener('display_animation_frame', previewListener);
        Blockbench.removeListener('select_animation', previewAnimationListener);
        Blockbench.removeListener('undo', previewUndoListener);
        Blockbench.removeListener('redo', previewUndoListener);
        Blockbench.removeListener('finish_edit', previewEditListener);
        destroySkillPreview();
        timelineMarkerSignature = '';
    }
});

function currentAnimation() {
    const animation = Animation.selected;
    if (!animation) {
        Blockbench.showMessageBox({
            title: 'No animation selected',
            message: 'Select or open an animation first, then place the playhead at the event time.',
            buttons: ['OK']
        });
        return null;
    }
    return animation;
}

function effectTimeline(animation, create) {
    let animator = animation.animators.effects;
    if (!animator && create) {
        animator = new EffectAnimator(animation);
        animation.animators.effects = animator;
    }
    if (!animator) return null;
    if (!Array.isArray(animator.timeline)) animator.timeline = [];
    return animator;
}

function snapSkillTime(time) {
    return Math.max(0, Math.round(Number(time || 0) * 20) / 20);
}

function parseMarker(script) {
    if (typeof script !== 'string') return null;
    if (script.startsWith(SWORDSKILL_MOLANG_MARKER_PREFIX) && script.endsWith(')')) {
        const terms = script.slice(SWORDSKILL_MOLANG_MARKER_PREFIX.length, -1).split('+').map(Number);
        if (terms.length < 1 || !Number.isSafeInteger(terms[0]) || terms[0] < 0 || terms[0] > 0xFFFFFF) {
            return null;
        }
        const byteLength = terms[0];
        const bytes = [];
        for (const term of terms.slice(1)) {
            if (!Number.isSafeInteger(term) || term < 0 || term > 0xFFFFFF) return null;
            bytes.push((term >>> 16) & 0xFF, (term >>> 8) & 0xFF, term & 0xFF);
        }
        if (byteLength > bytes.length) return null;
        const decoded = new TextDecoder().decode(new Uint8Array(bytes.slice(0, byteLength)));
        for (const prefix of [SWORDSKILL_META_PREFIX, SWORDSKILL_EVENT_PREFIX]) {
            if (!decoded.startsWith(prefix)) continue;
            try {
                return { prefix, data: JSON.parse(decoded.slice(prefix.length)) };
            } catch (error) {
                console.warn('Invalid SwordSkill timeline marker', error);
                return null;
            }
        }
        return null;
    }
    for (const prefix of [SWORDSKILL_META_PREFIX, SWORDSKILL_EVENT_PREFIX]) {
        if (script.startsWith(prefix)) {
            try {
                return { prefix, data: JSON.parse(script.slice(prefix.length)) };
            } catch (error) {
                console.warn('Invalid SwordSkill timeline marker', error);
                return null;
            }
        }
    }
    return null;
}

function encodeMarker(prefix, data) {
    const bytes = new TextEncoder().encode(prefix + JSON.stringify(data));
    const terms = [bytes.length];
    for (let index = 0; index < bytes.length; index += 3) {
        terms.push(((bytes[index] || 0) << 16) | ((bytes[index + 1] || 0) << 8) | (bytes[index + 2] || 0));
    }
    return `0*(${SWORDSKILL_MOLANG_MARKER_SENTINEL}+${terms.join('+')})`;
}

function migrateLegacyMarkers(targetAnimations = Array.isArray(Project?.animations) ? Project.animations : []) {
    const animations = targetAnimations;
    const updates = [];
    for (const animation of animations) {
        const animator = effectTimeline(animation, false);
        if (!animator) continue;
        for (const keyframe of animator.timeline) {
            const script = keyframe.data_points?.[0]?.script;
            const marker = parseMarker(script);
            if (marker && (script.startsWith(SWORDSKILL_META_PREFIX) || script.startsWith(SWORDSKILL_EVENT_PREFIX))) {
                updates.push({ animation, keyframe, marker });
            }
        }
    }
    if (!updates.length) return;

    const affectedAnimations = [...new Set(updates.map(update => update.animation))];
    Undo.initEdit({ animations: affectedAnimations });
    for (const { keyframe, marker } of updates) {
        keyframe.data_points[0].script = encodeMarker(marker.prefix, marker.data);
    }
    Undo.finishEdit('Convert SwordSkill timeline markers to valid Molang', { animations: affectedAnimations });
}

function getMarkers(animation) {
    const animator = effectTimeline(animation, false);
    if (!animator) return [];
    const entries = [];
    for (const keyframe of animator.timeline) {
        const marker = parseMarker(keyframe.data_points?.[0]?.script);
        if (!marker) continue;
        const records = unpackTimelineMarker(marker);
        records.forEach((record, entryIndex) => entries.push({
            keyframe, time: Number(keyframe.time),
            ...(records.length > 1 ? { entryIndex } : {}),
            marker: { prefix: record.prefix, data: record.data }
        }));
    }
    return entries;
}

function unpackTimelineMarker(marker) {
    if (marker.data.kind === 'bundle' && Array.isArray(marker.data.entries)) {
        return marker.data.entries.filter(record => record && typeof record.prefix === 'string'
            && record.data && typeof record.data === 'object');
    }
    if (marker.prefix === SWORDSKILL_EVENT_PREFIX && marker.data.kind === 'batch'
        && Array.isArray(marker.data.events)) {
        return marker.data.events.map(data => ({ prefix: SWORDSKILL_EVENT_PREFIX, data }));
    }
    return [{ prefix: marker.prefix, data: marker.data }];
}

function writeTimelineEntries(keyframe, records) {
    let marker;
    if (records.length === 1) {
        marker = records[0];
    } else if (records.every(record => record.prefix === SWORDSKILL_EVENT_PREFIX)) {
        marker = {
            prefix: SWORDSKILL_EVENT_PREFIX,
            data: { kind: 'batch', events: records.map(record => record.data) }
        };
    } else {
        marker = { prefix: SWORDSKILL_EVENT_PREFIX, data: { kind: 'bundle', entries: records } };
    }
    keyframe.data_points[0].script = encodeMarker(marker.prefix, marker.data);
}

function addTimelineRecord(animation, time, prefix, data) {
    const animator = effectTimeline(animation, true);
    time = snapSkillTime(time);
    animator.addToTimeline();
    const existing = animator.timeline.find(keyframe => Math.abs(Number(keyframe.time) - time) < 1e-6);
    if (existing) {
        const marker = parseMarker(existing.data_points[0].script);
        if (!marker || ![SWORDSKILL_META_PREFIX, SWORDSKILL_EVENT_PREFIX].includes(marker.prefix)) {
            throw new Error('This timeline time is occupied by another Effects instruction. Move it before adding SwordSkill data at the same time.');
        }
        const records = marker ? unpackTimelineMarker(marker) : [];
        records.push({ prefix, data });
        writeTimelineEntries(existing, records);
        return existing;
    }
    return animator.addKeyframe({
        channel: 'timeline', time,
        script: encodeMarker(prefix, data)
    });
}

function removeEventEntry(animation, entry) {
    const marker = parseMarker(entry.keyframe.data_points?.[0]?.script);
    if (!marker) return;
    const records = unpackTimelineMarker(marker);
    if (entry.entryIndex === undefined) {
        effectTimeline(animation, false)?.timeline.remove(entry.keyframe);
        return;
    }
    records.splice(entry.entryIndex, 1);
    if (records.length) writeTimelineEntries(entry.keyframe, records);
    else effectTimeline(animation, false)?.timeline.remove(entry.keyframe);
}

function updateTimelineEntry(entry, prefix, data) {
    if (entry.entryIndex === undefined) {
        entry.keyframe.data_points[0].script = encodeMarker(prefix, data);
        return;
    }
    const marker = parseMarker(entry.keyframe.data_points?.[0]?.script);
    const records = unpackTimelineMarker(marker);
    records[entry.entryIndex] = { prefix, data };
    writeTimelineEntries(entry.keyframe, records);
}

function saveEventEntry(animation, entry, time, data) {
    const keyTime = snapSkillTime(time);
    if (Math.abs(Number(entry.keyframe.time) - keyTime) < 1e-6) {
        updateTimelineEntry(entry, SWORDSKILL_EVENT_PREFIX, data);
        return entry.keyframe;
    }
    removeEventEntry(animation, entry);
    const keyframe = addTimelineRecord(animation, keyTime, SWORDSKILL_EVENT_PREFIX, data);
    animation.setLength(Math.max(animation.length || 0, keyTime));
    return keyframe;
}

function getMetadata(animation) {
    const entry = getMarkers(animation).find(item => item.marker.prefix === SWORDSKILL_META_PREFIX);
    return entry?.marker.data || null;
}

function initializeSkillPreview() {
    previewRoot = new THREE.Group();
    previewRoot.name = 'SwordSkill Preview';
    previewBoxRoot = new THREE.Group();
    previewRoot.add(previewBoxRoot);
    previewBoxMaterial = new THREE.MeshBasicMaterial({
        color: '#54D8FF',
        transparent: true,
        opacity: 0.18,
        depthWrite: false,
        depthTest: false
    });
    previewEdgeMaterial = new THREE.LineBasicMaterial({
        color: '#54D8FF',
        transparent: true,
        opacity: 0.95,
        depthTest: false
    });
    previewHandleMaterials = [0xFF5555, 0x55FF55, 0x5599FF].map(color => new THREE.MeshBasicMaterial({
        color,
        depthTest: false
    }));
    previewRotationMaterials = [0xFF5555, 0x55FF55, 0x5599FF].map(color => new THREE.MeshBasicMaterial({
        color,
        transparent: true,
        opacity: 0.95,
        depthTest: false
    }));
    Canvas.scene.add(previewRoot);
    previewListener = () => updateSkillPreview();
    previewAnimationListener = () => {
        migrateLegacyMarkers(Animation.selected ? [Animation.selected] : []);
        previewSignature = '';
        timelineMarkerSignature = '';
        updateSkillPreview(true);
    };
    previewUndoListener = () => {
        previewSignature = '';
        timelineMarkerSignature = '';
        updateSkillPreview(true);
    };
    Blockbench.on('display_animation_frame', previewListener);
    Blockbench.on('select_animation', previewAnimationListener);
    Blockbench.on('undo', previewUndoListener);
    Blockbench.on('redo', previewUndoListener);
    // Native timeline drag/delete operations finish through this event. Rebuild
    // derived ruler markers and preview from the actual keyframes afterwards.
    previewEditListener = () => {
        timelineMarkerSignature = '';
        previewSignature = '';
        updateSkillPreview(true);
    };
    Blockbench.on('finish_edit', previewEditListener);
    bindPreviewCanvas();
    updateSkillPreview(true);
}

function bindPreviewCanvas() {
    const selectedPreview = Preview.selected;
    const canvas = selectedPreview?.canvas || null;
    if (canvas === previewCanvas) {
        previewCamera = selectedPreview?.camPers || previewCamera;
        return;
    }
    if (previewCanvas) {
        previewCanvas.removeEventListener('pointerdown', previewPointerDownListener);
        previewCanvas.removeEventListener('pointermove', previewPointerMoveListener);
    }
    previewCanvas = canvas;
    previewCamera = selectedPreview?.camPers || null;
    createPreviewGizmoToolbar();
    if (!previewCanvas) return;
    previewPointerDownListener = startHitboxResize;
    previewPointerMoveListener = moveHitboxResize;
    previewPointerUpListener = finishHitboxResize;
    previewCanvas.addEventListener('pointerdown', previewPointerDownListener);
    previewCanvas.addEventListener('pointermove', previewPointerMoveListener);
    document.addEventListener('pointerup', previewPointerUpListener);
    document.addEventListener('pointercancel', previewPointerUpListener);
}

function destroySkillPreview() {
    if (!previewRoot) return;
    Canvas.scene.remove(previewRoot);
    clearPreviewBoxes();
    previewBoxMaterial?.dispose();
    previewEdgeMaterial?.dispose();
    previewHandleMaterials.forEach(material => material.dispose());
    previewRotationMaterials.forEach(material => material.dispose());
    previewHandleMaterials = [];
    previewRotationMaterials = [];
    if (previewCanvas) {
        previewCanvas.removeEventListener('pointerdown', previewPointerDownListener);
        previewCanvas.removeEventListener('pointermove', previewPointerMoveListener);
    }
    document.removeEventListener('pointerup', previewPointerUpListener);
    document.removeEventListener('pointercancel', previewPointerUpListener);
    previewCanvas = null;
    previewCamera = null;
    previewGizmoToolbar?.remove();
    previewGizmoToolbar = null;
    if (previewGizmoHost && previewCanvasParentPosition !== null) {
        previewGizmoHost.style.position = previewCanvasParentPosition;
    }
    previewGizmoHost = null;
    previewCanvasParentPosition = null;
    previewRoot = null;
    previewBoxRoot = null;
}

function createPreviewGizmoToolbar() {
    if (previewGizmoHost && previewGizmoHost !== previewCanvas?.parentElement
        && previewCanvasParentPosition !== null) {
        previewGizmoHost.style.position = previewCanvasParentPosition;
        previewGizmoHost = null;
        previewCanvasParentPosition = null;
    }
    previewGizmoToolbar?.remove();
    previewGizmoToolbar = null;
    const host = previewCanvas?.parentElement;
    if (!host) return;
    if (getComputedStyle(host).position === 'static') {
        previewCanvasParentPosition = host.style.position;
        host.style.position = 'relative';
    }
    previewGizmoHost = host;
    const toolbar = document.createElement('div');
    toolbar.className = 'swordskill-preview-gizmo-toolbar';
    Object.assign(toolbar.style, {
        position: 'absolute', top: '8px', left: '8px', zIndex: '20',
        display: 'flex', gap: '3px', padding: '3px', borderRadius: '4px',
        background: 'rgba(25, 25, 25, 0.82)', color: 'white',
        font: '12px sans-serif'
    });
    for (const [mode, label, title] of [
        ['move', '位置', 'Move selected hitbox'],
        ['resize', 'サイズ', 'Resize selected hitbox'],
        ['rotate', '回転', 'Rotate selected flying slash']
    ]) {
        const button = document.createElement('button');
        button.type = 'button';
        button.textContent = label;
        button.title = title;
        Object.assign(button.style, {
            border: '0', borderRadius: '3px', padding: '4px 7px',
            color: 'inherit', background: mode === previewGizmoMode ? '#3978c5' : 'transparent',
            cursor: 'pointer'
        });
        button.addEventListener('pointerdown', event => event.stopPropagation());
        button.addEventListener('click', event => {
            event.stopPropagation();
            previewGizmoMode = mode;
            createPreviewGizmoToolbar();
            updateSkillPreview(true);
        });
        toolbar.appendChild(button);
    }
    host.appendChild(toolbar);
    previewGizmoToolbar = toolbar;
}

function clearPreviewBoxes() {
    for (const box of previewBoxes) {
        previewBoxRoot.remove(box.mesh, box.edges, box.slash, box.path, ...box.handles, ...box.rotationHandles);
        box.mesh.geometry.dispose();
        box.edges.geometry.dispose();
        box.slash.geometry.dispose();
        box.slash.material.dispose();
        box.path.geometry.dispose();
        box.path.material.dispose();
        box.handles.forEach(handle => handle.geometry.dispose());
        box.rotationHandles.forEach(handle => handle.geometry.dispose());
    }
    previewBoxes = [];
}

function createSlashPreviewGeometry(size) {
    const radiusX = Math.max(0.35, size[0] * 0.5);
    const radiusZ = Math.max(0.25, size[1] * 0.5);
    const halfWidth = Math.max(0.06, Math.min(size[2] * 0.035, 0.2));
    const segments = 36;
    const positions = [];
    const indices = [];
    for (let index = 0; index <= segments; index++) {
        const angle = THREE.MathUtils.degToRad(-75 + 150 * index / segments);
        const sin = Math.sin(angle);
        const cos = Math.cos(angle);
        const centerX = radiusX * sin;
        // The Minecraft renderer's local +Z forward faces Blockbench's -Z model front.
        const centerZ = -radiusZ * cos;
        const normalX = -sin * halfWidth;
        const normalZ = -cos * halfWidth;
        positions.push(centerX + normalX, 0.025, centerZ + normalZ,
            centerX - normalX, 0.025, centerZ - normalZ);
        if (index < segments) {
            const base = index * 2;
            indices.push(base, base + 1, base + 3, base, base + 3, base + 2);
        }
    }
    const geometry = new THREE.BufferGeometry();
    geometry.setAttribute('position', new THREE.Float32BufferAttribute(positions, 3));
    geometry.setIndex(indices);
    geometry.computeVertexNormals();
    return geometry;
}

function projectilePreviewTravel(event, elapsedTicks) {
    const duration = Math.max(1, Number(event.duration_ticks ?? 12));
    const ticks = Math.max(0, Math.min(duration, elapsedTicks));
    const decaySum = (1 - Math.pow(0.99, ticks)) / 0.01;
    if (Array.isArray(event.movement)) {
        const scale = 0.8 * decaySum / duration;
        return new THREE.Vector3(
            Number(event.movement[0] || 0) * scale,
            Number(event.movement[1] || 0) * scale,
            -Number(event.movement[2] || 0) * scale
        );
    }
    const total = Math.max(0, Number(event.travel_distance ?? 16));
    const totalFactor = (1 - Math.pow(0.99, duration)) / 0.01;
    return new THREE.Vector3(0, 0, -total * decaySum / totalFactor);
}

function createProjectilePreviewPath(event, rotation, color) {
    const duration = Math.max(1, Math.min(1200, Number(event.duration_ticks ?? 12)));
    const points = [];
    const segments = Math.min(48, duration);
    for (let index = 0; index <= segments; index++) {
        points.push(projectilePreviewTravel(event, duration * index / segments));
    }
    const geometry = new THREE.BufferGeometry().setFromPoints(points);
    const path = new THREE.Line(geometry, new THREE.LineBasicMaterial({
        color, transparent: true, opacity: 0.62, depthTest: false
    }));
    path.renderOrder = 1000;
    path.quaternion.setFromEuler(new THREE.Euler(rotation[0], rotation[1], rotation[2], 'XYZ'));
    return path;
}

function updateSkillPreview(refreshCanvas = false) {
    if (!previewRoot) return;
    // Blockbench can create or replace the active preview after plugins load.
    // Keep resize interaction attached to the live canvas instead of capturing
    // whichever preview happened to be selected during onload.
    bindPreviewCanvas();
    const animation = Animation.selected;
    const metadata = animation ? getMetadata(animation) : null;
    const attackEntries = animation ? getMarkers(animation)
        .filter(entry => entry.marker.prefix === SWORDSKILL_EVENT_PREFIX
            && entry.marker.data.kind === 'attack')
        .sort((first, second) => first.time - second.time) : [];
    const projectileEntries = animation ? getMarkers(animation)
        .filter(entry => entry.marker.prefix === SWORDSKILL_EVENT_PREFIX
            && entry.marker.data.kind === 'projectile')
        .sort((first, second) => first.time - second.time) : [];
    const previewEntries = [
        ...attackEntries.map(entry => ({ entry, previewKind: 'attack' })),
        ...projectileEntries.map(entry => ({ entry, previewKind: 'projectile' }))
    ].sort((first, second) => first.entry.time - second.entry.time);
    const events = previewEntries.map(({ entry, previewKind }) => ({
        ...entry.marker.data,
        tick: Math.round(entry.time * 20),
        previewKind
    }));
    const signature = JSON.stringify({
        animation: animation?.uuid,
        color: metadata?.color,
        events
    });

    if (signature !== previewSignature) {
        clearPreviewBoxes();
        previewSignature = signature;
        const color = metadata?.color || '#54D8FF';
        previewBoxMaterial.color.set(color);
        previewEdgeMaterial.color.set(color);
        for (const event of events) {
            const size = event.size.map(value => Math.max(0.05, Number(value) * SWORDSKILL_PREVIEW_BLOCK_SIZE));
            const geometry = new THREE.BoxGeometry(size[0], size[1], size[2]);
            const mesh = new THREE.Mesh(geometry, previewBoxMaterial);
            mesh.renderOrder = 1001;
            const slashGeometry = event.previewKind === 'projectile' ? createSlashPreviewGeometry(size) : null;
            const slash = new THREE.Mesh(slashGeometry || new THREE.BufferGeometry(),
                new THREE.MeshBasicMaterial({
                    color, transparent: true, opacity: 0.82,
                    side: THREE.DoubleSide, depthWrite: false
                }));
            slash.visible = false;
            slash.renderOrder = 1003;
            const rotation = (event.rotation || [0, 0, 0])
                .map(value => THREE.MathUtils.degToRad(Number(value)));
            const path = event.previewKind === 'projectile'
                ? createProjectilePreviewPath(event, rotation, color)
                : new THREE.Line(new THREE.BufferGeometry(), new THREE.LineBasicMaterial({ color }));
            path.visible = event.previewKind === 'projectile';
            const edgeGeometry = new THREE.EdgesGeometry(geometry);
            const edges = new THREE.LineSegments(edgeGeometry, previewEdgeMaterial);
            edges.renderOrder = 1002;
            const handles = [0, 1, 2].map(axis => {
                const handle = new THREE.Mesh(new THREE.BoxGeometry(3, 3, 3), previewHandleMaterials[axis]);
                handle.renderOrder = 1003;
                handle.visible = false;
                previewBoxRoot.add(handle);
                return handle;
            });
            const rotationRadius = Math.max(...size) * 0.5 + 10;
            const rotationHandles = event.previewKind === 'projectile'
                ? [0, 1, 2].map(axis => {
                    const handle = new THREE.Mesh(
                        new THREE.TorusGeometry(rotationRadius, 1.2, 6, 64),
                        previewRotationMaterials[axis]
                    );
                    handle.renderOrder = 1004;
                    handle.visible = false;
                    previewBoxRoot.add(handle);
                    return handle;
                }) : [];
            previewBoxRoot.add(mesh, edges, slash, path);
            previewBoxes.push({
                mesh, edges, slash, path, handles, rotationHandles, event,
                entry: previewEntries[previewBoxes.length].entry
            });
        }
    }

    const currentTick = Math.round(Number(Timeline.time || 0) * 20);
    // Skill offsets are relative to the player's position and look direction,
    // not to an animated body bone. Use the root group's static pivot as the
    // preview anchor, but do not inherit its animated transform.
    const modelRoot = Project.groups.find(group => !(group.parent instanceof Group));
    const rootOrigin = modelRoot?.origin?.map(Number);
    const anchor = rootOrigin?.length === 3 && rootOrigin.every(Number.isFinite)
        ? rootOrigin : [0, 0, 0];
    for (const box of previewBoxes) {
        const selected = Boolean(box.entry?.keyframe?.selected);
        const isProjectile = box.event.previewKind === 'projectile';
        const visible = isProjectile ? selected : Number(box.event.tick) === currentTick || selected;
        const showHitbox = visible
            && (isProjectile || box.event.hitbox_enabled !== false);
        box.mesh.visible = showHitbox;
        box.edges.visible = showHitbox;
        // Keep the slash silhouette as a persistent guide; only the editable
        // collision volume and its handles depend on keyframe selection.
        box.slash.visible = isProjectile;
        const offset = box.event.offset.map(value => Number(value) * SWORDSKILL_PREVIEW_BLOCK_SIZE);
        // Minecraft's forward offset is +look; Blockbench's model-front is -Z.
        box.mesh.position.set(anchor[0] + offset[0], anchor[1] + offset[1], anchor[2] - offset[2]);
        if (box.event.previewKind === 'projectile') {
            const rotation = (box.event.rotation || [0, 0, 0]).map(value => THREE.MathUtils.degToRad(Number(value)));
            box.mesh.quaternion.setFromEuler(new THREE.Euler(rotation[0], rotation[1], rotation[2], 'XYZ'));
        } else {
            box.mesh.quaternion.identity();
        }
        box.edges.position.copy(box.mesh.position);
        box.edges.quaternion.copy(box.mesh.quaternion);
        box.edges.scale.copy(box.mesh.scale);
        box.slash.position.copy(box.mesh.position);
        box.slash.quaternion.copy(box.mesh.quaternion);
        box.path.position.copy(box.mesh.position);
        box.path.quaternion.copy(box.mesh.quaternion);
        if (isProjectile) {
            const elapsedTicks = currentTick - Number(box.event.tick);
            const travel = projectilePreviewTravel(box.event, Math.max(0, elapsedTicks));
            const duration = Math.max(1, Number(box.event.duration_ticks ?? 12));
            if (elapsedTicks >= 0 && elapsedTicks <= duration) {
                box.slash.position.add(travel.applyQuaternion(box.mesh.quaternion));
            }
        }
        box.handles.forEach(handle => { handle.visible = false; });
        box.rotationHandles.forEach(handle => { handle.visible = false; });
    }
    const selectedBox = previewBoxes.find(box => box.entry?.keyframe?.selected);
    if (selectedBox && selectedBox.mesh.visible) {
        selectedBox.handles.forEach((handle, axis) => {
            const localPosition = new THREE.Vector3();
            if (previewGizmoMode === 'move') {
                const reach = Math.max(...selectedBox.event.size.map(value =>
                    Number(value) * SWORDSKILL_PREVIEW_BLOCK_SIZE * 0.5)) + 5;
                localPosition.setComponent(axis, reach);
            } else {
                const halfSize = Number(selectedBox.event.size[axis]) * SWORDSKILL_PREVIEW_BLOCK_SIZE * 0.5;
                localPosition.setComponent(axis, halfSize + 2);
            }
            handle.position.copy(localPosition.applyQuaternion(selectedBox.mesh.quaternion)
                .add(selectedBox.mesh.position));
            handle.visible = previewGizmoMode !== 'rotate';
        });
        selectedBox.rotationHandles.forEach((handle, axis) => {
            const axisRotation = [
                new THREE.Quaternion().setFromEuler(new THREE.Euler(0, Math.PI / 2, 0)),
                new THREE.Quaternion().setFromEuler(new THREE.Euler(-Math.PI / 2, 0, 0)),
                new THREE.Quaternion()
            ][axis];
            handle.position.copy(selectedBox.mesh.position);
            handle.quaternion.copy(selectedBox.mesh.quaternion).multiply(axisRotation);
            handle.visible = previewGizmoMode === 'rotate';
        });
    }
    const eventEntries = animation ? getMarkers(animation)
        .filter(entry => entry.marker.prefix === SWORDSKILL_EVENT_PREFIX) : [];
    syncSkillTimelineMarkers(animation, eventEntries);
    if (refreshCanvas) Canvas.updateAll();
}

function getPointerNdc(event) {
    const rect = previewCanvas.getBoundingClientRect();
    return new THREE.Vector2(
        ((event.clientX - rect.left) / rect.width) * 2 - 1,
        -((event.clientY - rect.top) / rect.height) * 2 + 1
    );
}

function startHitboxResize(event) {
    if (event.button !== 0 || !previewCanvas || !previewCamera) return;
    const raycaster = new THREE.Raycaster();
    raycaster.setFromCamera(getPointerNdc(event), previewCamera);
    const rotationHandles = previewBoxes.flatMap(box => box.rotationHandles
        .map((handle, axis) => ({ box, handle, axis }))).filter(item => item.handle.visible);
    const rotationHit = raycaster.intersectObjects(rotationHandles.map(item => item.handle), false)[0];
    if (rotationHit) {
        const selectedRotation = rotationHandles.find(item => item.handle === rotationHit.object);
        if (selectedRotation) {
            const { box, axis } = selectedRotation;
            const center = projectToCanvas(box.mesh.position);
            const point = getPointerPosition(event);
            Undo.initEdit({ animations: [Animation.selected] });
            activeResize = {
                mode: 'rotate', box, axis, pointerId: event.pointerId,
                previousAngle: Math.atan2(point.y - center.y, point.x - center.x),
                initialRotation: [...(box.event.rotation || [0, 0, 0])],
                accumulatedAngle: 0
            };
            previewCanvas.setPointerCapture(event.pointerId);
            event.preventDefault();
            event.stopImmediatePropagation();
            return;
        }
    }
    const handles = previewBoxes.flatMap(box => box.handles.map((handle, axis) => ({ box, handle, axis })))
        .filter(item => item.handle.visible);
    const hit = raycaster.intersectObjects(handles.map(item => item.handle), false)[0];
    if (!hit) return;

    const selected = handles.find(item => item.handle === hit.object);
    if (!selected) return;
    const { box, axis } = selected;
    const axisWorld = new THREE.Vector3().setComponent(axis, 1).applyQuaternion(box.mesh.quaternion);
    const center = box.mesh.position.clone();
    const startScreen = projectToCanvas(center);
    const endScreen = projectToCanvas(center.clone().add(axisWorld.clone().multiplyScalar(SWORDSKILL_PREVIEW_BLOCK_SIZE)));
    const screenAxis = endScreen.sub(startScreen);
    const pixelsPerBlock = screenAxis.length();
    if (pixelsPerBlock < 0.001) return;

    Undo.initEdit({ animations: [Animation.selected] });
    activeResize = {
        mode: previewGizmoMode === 'move' ? 'move' : 'resize',
        box,
        axis,
        pointerId: event.pointerId,
        startX: event.clientX,
        startY: event.clientY,
        initialSize: Number(box.event.size[axis]),
        initialOffset: Number(box.event.offset[axis]),
        initialScale: box.mesh.scale.getComponent(axis),
        screenAxis: screenAxis.normalize(),
        pixelsPerBlock
    };
    previewCanvas.setPointerCapture(event.pointerId);
    event.preventDefault();
    event.stopImmediatePropagation();
}

function moveHitboxResize(event) {
    if (!activeResize || event.pointerId !== activeResize.pointerId) return;
    if (activeResize.mode === 'rotate') {
        const center = projectToCanvas(activeResize.box.mesh.position);
        const point = getPointerPosition(event);
        const angle = Math.atan2(point.y - center.y, point.x - center.x);
        let delta = angle - activeResize.previousAngle;
        if (delta > Math.PI) delta -= Math.PI * 2;
        if (delta < -Math.PI) delta += Math.PI * 2;
        activeResize.accumulatedAngle += delta;
        activeResize.previousAngle = angle;
        const rotation = [...activeResize.initialRotation];
        const rawAngle = activeResize.initialRotation[activeResize.axis]
            + THREE.MathUtils.radToDeg(activeResize.accumulatedAngle);
        // A 5 degree detent gives rotation drags a small, predictable pause
        // at useful angles while keeping the drag continuous between steps.
        rotation[activeResize.axis] = Math.round(rawAngle / 5) * 5;
        activeResize.box.event.rotation = rotation;
        applyPreviewRotation(activeResize.box);
        updateResizeHandles(activeResize.box);
        Canvas.updateAll();
        event.preventDefault();
        event.stopImmediatePropagation();
        return;
    }
    const deltaX = event.clientX - activeResize.startX;
    const deltaY = event.clientY - activeResize.startY;
    const projectedPixels = deltaX * activeResize.screenAxis.x + deltaY * activeResize.screenAxis.y;
    if (activeResize.mode === 'move') {
        const direction = activeResize.axis === 2 ? -1 : 1;
        const offset = [...activeResize.box.event.offset];
        offset[activeResize.axis] = Math.round((activeResize.initialOffset
            + direction * projectedPixels / activeResize.pixelsPerBlock) * 10) / 10;
        activeResize.box.event.offset = offset;
        const modelRoot = Project.groups.find(group => !(group.parent instanceof Group));
        const rootOrigin = modelRoot?.origin?.map(Number);
        const anchor = rootOrigin?.length === 3 && rootOrigin.every(Number.isFinite)
            ? rootOrigin : [0, 0, 0];
        activeResize.box.mesh.position.set(
            anchor[0] + offset[0] * SWORDSKILL_PREVIEW_BLOCK_SIZE,
            anchor[1] + offset[1] * SWORDSKILL_PREVIEW_BLOCK_SIZE,
            anchor[2] - offset[2] * SWORDSKILL_PREVIEW_BLOCK_SIZE
        );
        activeResize.box.edges.position.copy(activeResize.box.mesh.position);
        activeResize.box.path.position.copy(activeResize.box.mesh.position);
        activeResize.box.slash.position.copy(activeResize.box.mesh.position);
        const elapsedTicks = Math.round(Number(Timeline.time || 0) * 20)
            - Number(activeResize.box.event.tick);
        const duration = Math.max(1, Number(activeResize.box.event.duration_ticks ?? 12));
        if (elapsedTicks >= 0 && elapsedTicks <= duration) {
            const travel = projectilePreviewTravel(activeResize.box.event, elapsedTicks)
                .applyQuaternion(activeResize.box.mesh.quaternion);
            activeResize.box.slash.position.add(travel);
        }
        updateResizeHandles(activeResize.box);
        Canvas.updateAll();
        event.preventDefault();
        event.stopImmediatePropagation();
        return;
    }
    const size = Math.max(0.1, activeResize.initialSize + projectedPixels / activeResize.pixelsPerBlock * 2);
    activeResize.box.mesh.scale.setComponent(activeResize.axis,
        activeResize.initialScale * size / activeResize.initialSize);
    activeResize.box.edges.scale.copy(activeResize.box.mesh.scale);
    updateResizeHandles(activeResize.box);
    event.preventDefault();
    event.stopImmediatePropagation();
}

function finishHitboxResize(event) {
    if (!activeResize || (event.pointerId !== undefined && event.pointerId !== activeResize.pointerId)) return;
    const transformMode = activeResize.mode || 'resize';
    const { box, axis } = activeResize;
    if (transformMode === 'resize') {
        const size = [...box.event.size];
        size[axis] = Number(box.event.size[axis]) * box.mesh.scale.getComponent(axis);
        box.event.size = size;
    }
    const eventData = { ...box.event };
    delete eventData.previewKind;
    saveEventEntry(Animation.selected, box.entry, box.entry.time, eventData);
    const animation = Animation.selected;
    const metadata = animation ? getMetadata(animation) : null;
    if (metadata?.legacy_fallback) {
        const metadataEntry = getMarkers(animation).find(entry =>
            entry.marker.prefix === SWORDSKILL_META_PREFIX);
        if (metadataEntry) {
            metadataEntry.keyframe.data_points[0].script = encodeMarker(
                SWORDSKILL_META_PREFIX, { ...metadata, legacy_fallback: false });
        }
        Blockbench.showQuickMessage('Switched to data-driven mode. Add motion and attack events for the behavior you want.', 5000);
    }
    activeResize = null;
    const editLabel = transformMode === 'rotate' ? 'Rotate SwordSkill flying slash'
        : transformMode === 'move' ? 'Move SwordSkill hitbox' : 'Resize SwordSkill hitbox';
    Undo.finishEdit(editLabel, { animations: animation ? [animation] : [] });
    if (transformMode === 'rotate') {
        Blockbench.showQuickMessage(`Flying Slash RXYZ: ${box.event.rotation.map(value => Number(value).toFixed(1)).join(' / ')}°`, 3000);
    }
    previewSignature = '';
    updateSkillPreview(true);
    event.preventDefault?.();
    event.stopPropagation?.();
}

function getPointerPosition(event) {
    const rect = previewCanvas.getBoundingClientRect();
    return new THREE.Vector2(event.clientX - rect.left, event.clientY - rect.top);
}

function applyPreviewRotation(box) {
    const rotation = (box.event.rotation || [0, 0, 0])
        .map(value => THREE.MathUtils.degToRad(Number(value)));
    box.mesh.quaternion.setFromEuler(new THREE.Euler(rotation[0], rotation[1], rotation[2], 'XYZ'));
    box.edges.quaternion.copy(box.mesh.quaternion);
    box.slash.quaternion.copy(box.mesh.quaternion);
    box.path.quaternion.copy(box.mesh.quaternion);
    box.rotationHandles.forEach((handle, axis) => {
        const axisRotation = [
            new THREE.Quaternion().setFromEuler(new THREE.Euler(0, Math.PI / 2, 0)),
            new THREE.Quaternion().setFromEuler(new THREE.Euler(-Math.PI / 2, 0, 0)),
            new THREE.Quaternion()
        ][axis];
        handle.position.copy(box.mesh.position);
        handle.quaternion.copy(box.mesh.quaternion).multiply(axisRotation);
    });
}

function updateResizeHandles(box) {
    box.handles.forEach((handle, axis) => {
        if (!handle.visible) return;
        const halfSize = Number(box.event.size[axis]) * SWORDSKILL_PREVIEW_BLOCK_SIZE * box.mesh.scale.getComponent(axis) * 0.5;
        const localPosition = new THREE.Vector3(0, 0, 0);
        localPosition.setComponent(axis, halfSize + 2);
        handle.position.copy(localPosition.applyQuaternion(box.mesh.quaternion).add(box.mesh.position));
    });
}

function projectToCanvas(point) {
    previewCamera.updateMatrixWorld(true);
    const projected = point.clone().project(previewCamera);
    const rect = previewCanvas.getBoundingClientRect();
    return new THREE.Vector2(
        (projected.x + 1) * 0.5 * rect.width,
        (1 - projected.y) * 0.5 * rect.height
    );
}

function syncSkillTimelineMarkers(animation, eventEntries) {
    if (!animation) {
        timelineMarkerSignature = '';
        return;
    }
    const desired = [];
    for (const entry of eventEntries) {
        const kind = entry.marker.data.kind;
        const baseLabel = SWORDSKILL_EVENT_MARKER_LABELS[kind];
        const label = entry.marker.data.trigger === 'CONTACT'
            ? `Contact +${Math.round(Number(entry.marker.data.tick ?? entry.time * 20))}t ${baseLabel}`
            : baseLabel;
        if (!label || !Number.isFinite(entry.time)) continue;
        const existing = desired.find(marker => Math.abs(marker.time - entry.time) < 0.001);
        if (existing) {
            existing.count++;
            if (!existing.kinds.includes(label)) existing.kinds.push(label);
        } else {
            desired.push({ time: entry.time, count: 1, kinds: [label] });
        }
    }
    desired.forEach(marker => {
        marker.tick = Math.round(marker.time * 20);
        marker.name = `${SWORDSKILL_TIMELINE_MARKER_PREFIX}${marker.tick}t (${[...marker.kinds].join(', ')})`
            + (marker.count > 1 ? ` ×${marker.count}` : '');
        marker.color = 1;
    });
    const signature = JSON.stringify({ animation: animation.uuid, desired });
    if (signature === timelineMarkerSignature) return;

    const ownedPrefixes = [SWORDSKILL_TIMELINE_MARKER_PREFIX, '[SwordSkill Hitbox] ',
        ...Object.values(SWORDSKILL_EVENT_MARKER_LABELS)
            .filter(label => label !== 'Hitbox')
            .map(label => `[SwordSkill ${label}] `)];
    if (!Array.isArray(animation.markers)) animation.markers = [];
    for (let index = animation.markers.length - 1; index >= 0; index--) {
        const marker = animation.markers[index];
        if (typeof marker.name === 'string' && ownedPrefixes.some(prefix => marker.name.startsWith(prefix))) {
            animation.markers.splice(index, 1);
        }
    }
    for (const marker of desired) {
        const timelineMarker = new TimelineMarker();
        timelineMarker.time = marker.time;
        timelineMarker.color = marker.color;
        // Recent Blockbench versions support names; older versions ignore this
        // extra property while still showing the colored ruler marker.
        timelineMarker.name = marker.name;
        timelineMarker._swordskillTick = marker.tick;
        timelineMarker._swordskillTime = marker.time;
        animation.markers.push(timelineMarker);
    }
    timelineMarkerSignature = signature;
    Timeline.vue?.$forceUpdate?.();
}

function addMarker(animation, time, prefix, data) {
    const animator = effectTimeline(animation, true);
    time = snapSkillTime(time);
    animator.addToTimeline();
    const keyframe = addTimelineRecord(animation, time, prefix, data);
    animation.setLength(Math.max(animation.length || 0, time));
    animation.selected = true;
    Timeline.vue?.$forceUpdate?.();
    Undo.finishEdit('Add SwordSkill timeline event', { animations: [animation] });
    updateSkillPreview(true);
    return keyframe;
}

function setupSkillMetadata() {
    const animation = currentAnimation();
    if (!animation) return;
    const existing = getMetadata(animation) || {};
    const currentAnimationName = String(animation.name || 'new_skill')
        .replace(/\.(start|move|finish)$/i, '');
    const currentName = currentAnimationName.toLowerCase().replace(/[^a-z0-9_]/g, '_');
    const existingWeapons = Array.isArray(existing.weapons)
        ? existing.weapons
        : String(existing.weapons || 'ONE_HANDED_SWORD').split(',');
    const normalizedWeapons = existingWeapons.map(weapon => String(weapon).trim().toUpperCase()).filter(Boolean);
    const rushCombo = existing.rush_combo || {};
    const rushStartMotion = rushCombo.start_motion || {};
    const rushContactMotion = rushCombo.contact_motion || {};
    const values = {
        id: existing.id ?? 0,
        replace_builtin: existing.replace_builtin ?? false,
        legacy_fallback: existing.legacy_fallback ?? existing.replace_builtin ?? false,
        name: existing.name || currentName || 'new_skill',
        animation: existing.animation || currentAnimationName,
        type: existing.type || 'SIMPLE',
        cooldown: existing.cooldown ?? 60,
        sp_cost: existing.sp_cost ?? 8,
        duration_ticks: existing.duration_ticks ?? Math.max(1, Math.round(Number(animation.length || 0) * 20)),
        transform_limit_ticks: existing.transform_limit_ticks ?? 20,
        rush_start_ticks: existing.rush_start_ticks ?? 5,
        rush_start_animation: existing.rush_start_animation || 'start',
        rush_move_animation: existing.rush_move_animation || 'move',
        rush_finish_animation: existing.rush_finish_animation || 'finish',
        rush_combo: existing.rush_combo ?? null,
        rush_combo_enabled: Boolean(existing.rush_combo),
        rush_start_forward: rushStartMotion.forward ?? 3.5,
        rush_start_vertical: rushStartMotion.vertical ?? 0.25,
        rush_pitch_limit: rushStartMotion.pitch_limit_degrees ?? 15,
        rush_use_charge_multiplier: rushCombo.use_charge_multiplier ?? true,
        rush_invulnerability: rushCombo.invulnerability_ticks ?? 35,
        rush_search_radius: rushCombo.contact_search_radius ?? 8,
        rush_contact_radius: rushCombo.contact_radius ?? 1.5,
        rush_contact_forward: rushContactMotion.forward ?? -0.5,
        rush_latest_contact: rushCombo.latest_contact_tick
            ?? Math.min(existing.duration_ticks ?? Math.max(1, Math.round(Number(animation.length || 0) * 20)), 35),
        rush_no_contact_timeout: rushCombo.no_contact_timeout_tick ?? 21,
        rush_contact_animation_offset: rushCombo.contact_animation_offset ?? 1,
        rush_finish_contact_offset: rushCombo.finish_contact_offset ?? 18,
        hidden: existing.hidden ?? false,
        color: existing.color || '#54D8FF',
        trail_length: existing.trail_length ?? 1.5,
        shockwave_ticks: existing.shockwave_ticks ?? 0,
        hitstop_ticks: existing.hitstop_ticks ?? -1
    };
    const weaponFields = {};
    for (const weapon of SWORDSKILL_WEAPONS) {
        weaponFields[`weapon_${weapon}`] = {
            label: weapon,
            type: 'checkbox',
            value: normalizedWeapons.includes(weapon)
        };
    }
    weaponFields.weapons_custom = {
        label: 'Additional weapon types (comma separated)',
        type: 'text',
        value: normalizedWeapons.filter(weapon => !SWORDSKILL_WEAPONS.includes(weapon)).join(', '),
        description: 'You may type supported weapon type names here instead of selecting them above.'
    };

    new Dialog({
        id: 'swordskill_timeline_metadata',
        title: `Skill Metadata: ${animation.name}`,
        form: {
            id: {
                label: 'Skill ID (0 = auto-assign, or target ID when replacing)',
                type: 'number',
                value: values.id,
                step: 1,
                description: 'Use 0 for auto-assign, or enable built-in replacement to preserve an existing skill ID.'
            },
            replace_builtin: {
                label: 'Replace built-in skill at this ID',
                type: 'checkbox',
                value: values.replace_builtin,
                description: 'Use only when migrating a registered Java skill. Its ID, slot assignments, and unlocks are preserved.'
            },
            legacy_fallback: {
                label: 'Keep original Java skill behavior',
                type: 'checkbox',
                value: values.legacy_fallback,
                description: 'Runs the original Java skill instead of skilldata events. Turn off after converting its combat behavior.'
            },
            name: {
                label: 'Skill registry name (auto-filled)',
                type: 'text',
                value: values.name,
                description: 'Generated from the animation name. Usually leave unchanged.'
            },
            animation: {
                label: 'Animation base name',
                type: 'text',
                value: values.animation,
                description: 'Rush animation resource names use this base followed by each phase suffix.'
            },
            type: {
                label: 'Skill type',
                type: 'select',
                options: { SIMPLE: 'SIMPLE', TRANSFORM: 'TRANSFORM', TRANSFORM_FINISH: 'TRANSFORM_FINISH', RUSH: 'RUSH' },
                value: values.type
            },
            ...weaponFields,
            cooldown: { label: 'Cooldown (ticks)', type: 'number', value: values.cooldown, min: 0, step: 1 },
            sp_cost: { label: 'SP cost', type: 'number', value: values.sp_cost, min: 0, step: 0.5 },
            duration_ticks: { label: 'Skill duration (ticks)', type: 'number', value: values.duration_ticks, min: 1, max: 1200, step: 1 },
            transform_limit_ticks: { label: 'Combo input window (ticks)', type: 'number', value: values.transform_limit_ticks, min: 0, step: 1 },
            rush_start_ticks: {
                label: 'Rush: startup duration (ticks)',
                type: 'number',
                value: values.rush_start_ticks,
                min: 0,
                step: 1,
                description: 'Time spent in startup before the move animation begins.'
            },
            rush_start_animation: { label: 'Rush: startup animation suffix', type: 'text', value: values.rush_start_animation },
            rush_move_animation: { label: 'Rush: movement animation suffix', type: 'text', value: values.rush_move_animation },
            rush_finish_animation: { label: 'Rush: finish animation suffix', type: 'text', value: values.rush_finish_animation },
            rush_combo_enabled: {
                label: 'RUSH: enable contact-triggered combo events', type: 'checkbox',
                value: values.rush_combo_enabled
            },
            rush_start_forward: { label: 'RUSH combo: start forward speed', type: 'number', value: values.rush_start_forward, step: 0.1 },
            rush_start_vertical: { label: 'RUSH combo: start vertical speed', type: 'number', value: values.rush_start_vertical, step: 0.1 },
            rush_pitch_limit: { label: 'RUSH combo: pitch limit (degrees)', type: 'number', value: values.rush_pitch_limit, min: 0, max: 90, step: 1 },
            rush_use_charge_multiplier: { label: 'RUSH combo: scale damage by charge level', type: 'checkbox', value: values.rush_use_charge_multiplier },
            rush_invulnerability: { label: 'RUSH combo: invulnerability ticks', type: 'number', value: values.rush_invulnerability, min: 0, max: 1200, step: 1 },
            rush_search_radius: { label: 'RUSH combo: contact search radius', type: 'number', value: values.rush_search_radius, min: 0.1, max: 64, step: 0.1 },
            rush_contact_radius: { label: 'RUSH combo: contact radius', type: 'number', value: values.rush_contact_radius, min: 0.1, max: 64, step: 0.1 },
            rush_contact_forward: { label: 'RUSH combo: reverse speed on contact', type: 'number', value: values.rush_contact_forward, min: -16, max: 16, step: 0.1 },
            rush_latest_contact: { label: 'RUSH combo: latest contact tick', type: 'number', value: values.rush_latest_contact, min: 1, step: 1 },
            rush_no_contact_timeout: { label: 'RUSH combo: timeout without contact', type: 'number', value: values.rush_no_contact_timeout, min: 1, step: 1 },
            rush_contact_animation_offset: { label: 'RUSH combo: finish animation delay after contact', type: 'number', value: values.rush_contact_animation_offset, min: 0, step: 1 },
            rush_finish_contact_offset: { label: 'RUSH combo: end delay after contact', type: 'number', value: values.rush_finish_contact_offset, min: 1, step: 1 },
            hidden: { label: 'Hide from skill selection', type: 'checkbox', value: values.hidden },
            color: { label: 'Skill and weapon trail color', type: 'color', value: values.color },
            trail_length: {
                label: 'Weapon trail length (default 1.5)',
                type: 'number',
                value: values.trail_length,
                min: 0.1,
                max: 32,
                step: 0.1,
                description: 'Longer values emphasise thrusting skills.'
            },
            shockwave_ticks: {
                label: 'Shockwave duration (ticks, 0 = off)',
                type: 'number',
                value: values.shockwave_ticks,
                min: 0,
                max: 1200,
                step: 1,
                description: 'Draws a shockwave along the thrust beam, tinted with the skill colour.'
            },
            hitstop_ticks: {
                label: 'Hit-stop ticks (-1 = server default, 0 = off)',
                type: 'number',
                value: values.hitstop_ticks,
                min: -1,
                max: 40,
                step: 1,
                description: 'Freezes the attacker and target briefly on hit.'
            }
        },
        onConfirm(form) {
            try {
                const metadata = buildMetadata(form, animation);
                const oldEntry = getMarkers(animation).find(item => item.marker.prefix === SWORDSKILL_META_PREFIX);
                Undo.initEdit({ animations: [animation] });
                if (oldEntry) {
                    updateTimelineEntry(oldEntry, SWORDSKILL_META_PREFIX, metadata);
                    Undo.finishEdit('Update SwordSkill metadata', { animations: [animation] });
                    updateSkillPreview(true);
                } else {
                    addMarker(animation, 0, SWORDSKILL_META_PREFIX, metadata);
                }
            } catch (error) {
                showError(error);
                return false;
            }
        }
    }).show();
}

function buildMetadata(form, animation) {
    const idText = String(form.id ?? '').trim();
    const replaceBuiltin = Boolean(form.replace_builtin);
    let id = 0;
    if (idText) {
        id = Number(idText);
        if (!Number.isSafeInteger(id) || id < 0
            || (replaceBuiltin ? id >= SWORDSKILL_FIRST_CUSTOM_ID : (id !== 0 && id < SWORDSKILL_FIRST_CUSTOM_ID))) {
            throw new Error(replaceBuiltin
                ? 'Built-in replacement requires an existing skill ID from 0 to 84.'
                : `Skill ID must be 0 (auto-assign) or a whole number starting at ${SWORDSKILL_FIRST_CUSTOM_ID}.`);
        }
    }
    const name = String(form.name || '').trim();
    if (!/^[a-z0-9_]+$/.test(name)) throw new Error('Skill name must use lowercase letters, digits, and underscores.');
    const animationBaseName = String(form.animation || '').trim();
    if (!/^[a-z0-9_]+$/.test(animationBaseName)) throw new Error('Animation base name must use lowercase letters, digits, and underscores.');
    const rushAnimationSuffixes = ['rush_start_animation', 'rush_move_animation', 'rush_finish_animation']
        .map(field => String(form[field] || '').trim());
    if (rushAnimationSuffixes.some(suffix => !/^[a-z0-9_]+$/.test(suffix))) {
        throw new Error('Rush animation suffixes must use lowercase letters, digits, and underscores.');
    }
    const rushStartTicks = Math.max(0, Math.floor(Number(form.rush_start_ticks)));
    if (!Number.isSafeInteger(rushStartTicks)) throw new Error('Rush startup duration must be a whole number of ticks.');
    const durationTicks = Math.floor(Number(form.duration_ticks));
    if (!Number.isSafeInteger(durationTicks) || durationTicks < 1 || durationTicks > 1200) {
        throw new Error('Skill duration must be a whole number from 1 to 1200 ticks.');
    }
    let rushCombo = null;
    if (form.rush_combo_enabled) {
        if (String(form.type || 'SIMPLE') !== 'RUSH') {
            throw new Error('Contact-triggered combo events require skill type RUSH.');
        }
        const values = ['rush_start_forward', 'rush_start_vertical', 'rush_pitch_limit',
            'rush_invulnerability', 'rush_search_radius', 'rush_contact_radius', 'rush_contact_forward',
            'rush_latest_contact', 'rush_no_contact_timeout', 'rush_contact_animation_offset',
            'rush_finish_contact_offset'].map(field => Number(form[field]));
        if (!values.every(Number.isFinite) || values[2] < 0 || values[2] > 90
            || values[3] < 0 || values[3] > 1200 || values[4] <= 0 || values[4] > 64
            || values[5] <= 0 || values[5] > values[4] || Math.abs(values[6]) > 16
            || !Number.isSafeInteger(values[7]) || values[7] < 1 || values[7] > durationTicks
            || !Number.isSafeInteger(values[8]) || values[8] < 1 || values[8] > durationTicks + 1
            || !Number.isSafeInteger(values[9]) || values[9] < 0 || values[9] > durationTicks
            || !Number.isSafeInteger(values[10]) || values[10] < values[9] || values[10] > durationTicks
            || values[7] + values[10] > durationTicks) {
            throw new Error('RUSH combo settings are outside the supported ranges.');
        }
        rushCombo = {
            start_motion: { forward: values[0], vertical: values[1], pitch_limit_degrees: values[2] },
            use_charge_multiplier: Boolean(form.rush_use_charge_multiplier),
            invulnerability_ticks: values[3],
            contact_search_radius: values[4],
            contact_radius: values[5],
            contact_motion: { forward: values[6] },
            latest_contact_tick: values[7],
            no_contact_timeout_tick: values[8],
            contact_animation_offset: values[9],
            finish_contact_offset: values[10]
        };
    }

    const weapons = SWORDSKILL_WEAPONS.filter(weapon => Boolean(form[`weapon_${weapon}`]));
    const typedWeapons = String(form.weapons_custom || '')
        .split(',')
        .map(value => value.trim().toUpperCase())
        .filter(Boolean);
    for (const weapon of typedWeapons) {
        if (!SWORDSKILL_WEAPONS.includes(weapon)) {
            throw new Error(`Unknown weapon type "${weapon}". Supported types: ${SWORDSKILL_WEAPONS.join(', ')}`);
        }
        if (!weapons.includes(weapon)) weapons.push(weapon);
    }
    if (!weapons.length || weapons.some(weapon => !SWORDSKILL_WEAPONS.includes(weapon))) {
        throw new Error(`Choose or enter one or more weapon types from: ${SWORDSKILL_WEAPONS.join(', ')}`);
    }
    const color = String(form.color || '').replace(/^#/, '');
    if (!/^[0-9a-fA-F]{6}$/.test(color)) throw new Error('Choose a valid RGB color.');

    const trailLength = Number(form.trail_length ?? 1.5);
    if (!Number.isFinite(trailLength) || trailLength <= 0 || trailLength > 32) {
        throw new Error('Weapon trail length must be greater than 0 and at most 32.');
    }
    const shockwaveTicks = Math.floor(Number(form.shockwave_ticks ?? 0));
    if (!Number.isSafeInteger(shockwaveTicks) || shockwaveTicks < 0 || shockwaveTicks > 1200) {
        throw new Error('Shockwave duration must be a whole number from 0 to 1200 ticks.');
    }
    const hitstopTicks = Math.floor(Number(form.hitstop_ticks ?? -1));
    if (!Number.isSafeInteger(hitstopTicks) || hitstopTicks < -1 || hitstopTicks > 40) {
        throw new Error('Hit-stop ticks must be -1 (server default), 0 (disabled), or up to 40.');
    }

    const metadata = {
        name,
        animation: animationBaseName,
        replace_builtin: replaceBuiltin,
        legacy_fallback: Boolean(form.legacy_fallback),
        type: String(form.type || 'SIMPLE'),
        weapons,
        cooldown: Math.max(0, Math.floor(Number(form.cooldown || 0))),
        sp_cost: Math.max(0, Number(form.sp_cost || 0)),
        duration_ticks: durationTicks,
        transform_limit_ticks: Math.max(0, Math.floor(Number(form.transform_limit_ticks || 0))),
        rush_start_ticks: rushStartTicks,
        rush_start_animation: rushAnimationSuffixes[0],
        rush_move_animation: rushAnimationSuffixes[1],
        rush_finish_animation: rushAnimationSuffixes[2],
        rush_combo: rushCombo,
        hidden: Boolean(form.hidden),
        color: `#${color.toUpperCase()}`,
        trail_length: trailLength,
        shockwave_ticks: shockwaveTicks,
        hitstop_ticks: hitstopTicks
    };
    metadata.id = id;
    return metadata;
}

function openTimelineEventDialog(kind, existingEntry = null) {
    const animation = currentAnimation();
    if (!animation) return;
    if (!getMetadata(animation)) {
        Blockbench.showMessageBox({
            title: 'Skill metadata required',
            message: 'Run Tools > SwordSkill: Set Skill Metadata before adding timeline events.',
            buttons: ['OK']
        });
        return;
    }

    const data = existingEntry?.marker.data || {};
    const time = existingEntry ? existingEntry.time : Number(Timeline.time || 0);
    const tick = Math.round(time * 20);
    const attack = kind === 'attack';
    const projectile = kind === 'projectile';
    const soundOnly = kind === 'sound';
    const effectOptions = {
        none: 'None',
        undead_bonus: 'Bonus damage vs undead',
        low_hp_bonus: 'Bonus damage vs low HP targets',
        magic_bonus: 'Additional magic damage',
        stun: 'Stun (strong movement slowdown)',
        lifesteal: 'Lifesteal',
        custom: 'Custom registered effect'
    };
    const attackSoundOptions = {
        '': 'None',
        'minecraft:entity.player.attack.sweep': 'Player attack sweep',
        'minecraft:entity.player.attack.strong': 'Player attack strong',
        'minecraft:entity.player.attack.crit': 'Player attack critical',
        'minecraft:entity.player.attack.knockback': 'Player attack knockback',
        'minecraft:entity.player.attack.weak': 'Player attack weak',
        'minecraft:entity.trident.throw': 'Trident throw',
        'minecraft:entity.trident.hit': 'Trident hit'
    };
    const savedSound = String(data.sound || '').trim();
    const savedSoundIsPreset = Object.prototype.hasOwnProperty.call(attackSoundOptions, savedSound);
    const firstEffect = Array.isArray(data.effects) ? data.effects[0] : null;
    const firstEffectType = firstEffect?.type;
    const initialEffect = !firstEffectType ? 'none'
        : (Object.prototype.hasOwnProperty.call(effectOptions, firstEffectType) ? firstEffectType : 'custom');
    let form = {
        hitbox_enabled: { label: 'Enable damage hitbox', type: 'checkbox', value: data.hitbox_enabled !== false },
        trigger: {
            label: 'RUSH event timing',
            type: 'select',
            options: { TICK: 'From skill start', CONTACT: 'After contact (relative ticks)' },
            value: data.trigger || 'TICK'
        },
        offset_x: { label: 'Hitbox offset: right', type: 'number', value: data.offset?.[0] ?? 0, step: 0.1 },
        offset_y: { label: 'Hitbox offset: up', type: 'number', value: data.offset?.[1] ?? 0, step: 0.1 },
        offset_z: { label: 'Hitbox offset: forward', type: 'number', value: data.offset?.[2] ?? 2, step: 0.1 },
        size_x: { label: 'Hitbox width', type: 'number', value: data.size?.[0] ?? 4, min: 0.1, step: 0.1 },
        size_y: { label: 'Hitbox height', type: 'number', value: data.size?.[1] ?? 3, min: 0.1, step: 0.1 },
        size_z: { label: 'Hitbox depth', type: 'number', value: data.size?.[2] ?? 4, min: 0.1, step: 0.1 },
        rotation_x: { label: 'Flying slash pitch offset', type: 'number', value: data.rotation?.[0] ?? 0, step: 1 },
        rotation_y: { label: 'Flying slash yaw offset', type: 'number', value: data.rotation?.[1] ?? 0, step: 1 },
        rotation_z: { label: 'Flying slash roll', type: 'number', value: data.rotation?.[2] ?? 0, step: 1 },
        player_pitch_x_multiplier: { label: 'Follow player pitch (X)', type: 'number', value: data.player_pitch_x_multiplier ?? 0, min: -4, max: 4, step: 0.1 },
        player_pitch_z_multiplier: { label: 'Follow player pitch (Z)', type: 'number', value: data.player_pitch_z_multiplier ?? 0, min: -4, max: 4, step: 0.1 },
        horizontal: { label: 'Keep flying slash horizontal', type: 'checkbox', value: Boolean(data.horizontal) },
        render_pitch: { label: 'Flying slash render pitch', type: 'number', value: data.render_pitch ?? 0, min: -180, max: 180, step: 1 },
        damage_multiplier: { label: 'Damage multiplier', type: 'number', value: data.damage_multiplier ?? 2, min: 0, step: 0.1 },
        damage_type: {
            label: 'Damage base',
            type: 'select',
            options: { BASE: 'Base attack damage', RUSH: 'Movement-based rush damage' },
            value: data.damage_type || 'BASE'
        },
        knockback: { label: 'Target knockback', type: 'number', value: data.knockback ?? 0.5, min: -16, max: 16, step: 0.1 },
        use_knockback_multiplier: {
            label: 'Scale knockback by player knockback attribute',
            type: 'checkbox',
            value: Number(data.knockback_multiplier ?? -1) !== -1
        },
        knockback_multiplier: {
            label: 'Knockback attribute multiplier',
            type: 'number',
            value: data.knockback_multiplier !== -1 ? data.knockback_multiplier : 1,
            min: -16,
            max: 16,
            step: 0.1
        },
        repeat_while_rushing: {
            label: 'Repeat each tick until a RUSH hit lands',
            type: 'checkbox',
            value: Boolean(data.repeat_while_rushing)
        },
        duration_ticks: { label: 'Flying slash lifetime (ticks)', type: 'number', value: data.duration_ticks ?? 12, min: 1, max: 1200, step: 1 },
        travel_distance: { label: 'Flying slash travel distance', type: 'number', value: data.travel_distance ?? 16, min: 0, max: 128, step: 0.5 },
        use_movement: { label: 'Use legacy movement vector (overrides travel distance)', type: 'checkbox', value: Array.isArray(data.movement) },
        movement_x: { label: 'Movement: right', type: 'number', value: data.movement?.[0] ?? 0, step: 0.1 },
        movement_y: { label: 'Movement: up', type: 'number', value: data.movement?.[1] ?? 0, step: 0.1 },
        movement_z: { label: 'Movement: forward', type: 'number', value: data.movement?.[2] ?? 0, step: 0.1 },
        particle: { label: 'Slash effect key (legacy particle key)', type: 'text', value: data.particle || 'skill_particle_sword/blue' },
        effect: { label: 'Effect on hit', type: 'select', options: effectOptions, value: initialEffect },
        effect_multiplier: { label: 'Effect multiplier (bonus damage / lifesteal / magic)', type: 'number', value: data.effects?.[0]?.multiplier ?? 2, min: 0, step: 0.1 },
        effect_duration: { label: 'Effect duration (stun ticks)', type: 'number', value: data.effects?.[0]?.duration_ticks ?? 40, min: 1, step: 1 },
        effect_amplifier: { label: 'Effect amplifier (stun level, 0-based)', type: 'number', value: data.effects?.[0]?.amplifier ?? 0, min: 0, step: 1 },
        custom_effect: { label: 'Custom Java effect handler ID', type: 'text', value: initialEffect === 'custom' ? (firstEffectType || '') : '' },
        effect_extra: {
            label: 'Additional effect entries (JSON array)',
            type: 'textarea',
            value: Array.isArray(data.effects) && data.effects.length > 1
                ? JSON.stringify(data.effects.slice(1)) : '',
            description: 'Extra effects beyond the first, e.g. [{"type":"lifesteal","multiplier":0.25}]'
        },
        sound_preset: {
            label: 'Sound preset',
            type: 'select',
            options: attackSoundOptions,
            value: savedSoundIsPreset ? savedSound : ''
        },
        sound_custom: {
            label: 'Sound ID',
            type: 'text',
            value: savedSoundIsPreset ? '' : savedSound,
            description: 'Registered sound ID. Example: minecraft:entity.player.attack.sweep'
        },
        sound_volume: { label: 'Sound volume', type: 'number', value: data.volume ?? 1, min: 0, max: 16, step: 0.1 },
        sound_pitch: { label: 'Sound pitch', type: 'number', value: data.pitch ?? 1, min: 0.5, max: 2, step: 0.05 },
        forward: { label: 'User movement: forward', type: 'number', value: data.forward ?? 0, step: 0.1 },
        strafe: { label: 'User movement: right', type: 'number', value: data.strafe ?? 0, step: 0.1 },
        vertical: { label: 'User movement: up', type: 'number', value: data.vertical ?? 0, step: 0.1 }
    };
    if (attack || projectile) {
        delete form.forward;
        delete form.strafe;
        delete form.vertical;
    } else {
        delete form.offset_x;
        delete form.offset_y;
        delete form.offset_z;
        delete form.size_x;
        delete form.size_y;
        delete form.size_z;
        delete form.damage_multiplier;
        delete form.knockback;
        delete form.use_knockback_multiplier;
        delete form.knockback_multiplier;
        delete form.damage_type;
        delete form.effect;
        delete form.effect_multiplier;
        delete form.effect_duration;
        delete form.effect_amplifier;
        delete form.custom_effect;
        delete form.effect_extra;
        if (!soundOnly) {
            delete form.sound_preset;
            delete form.sound_custom;
        }
    }
    if (projectile) {
        delete form.hitbox_enabled;
        delete form.effect;
        delete form.effect_multiplier;
        delete form.effect_duration;
        delete form.effect_amplifier;
        delete form.custom_effect;
        delete form.effect_extra;
    } else {
        delete form.rotation_x;
        delete form.rotation_y;
        delete form.rotation_z;
        delete form.player_pitch_x_multiplier;
        delete form.player_pitch_z_multiplier;
        delete form.horizontal;
        delete form.render_pitch;
        delete form.duration_ticks;
        delete form.travel_distance;
        delete form.particle;
        delete form.use_movement;
        delete form.movement_x;
        delete form.movement_y;
        delete form.movement_z;
    }
    if (!attack || getMetadata(animation)?.type !== 'RUSH') {
        delete form.repeat_while_rushing;
    }
    if ((!attack && !projectile) || getMetadata(animation)?.type !== 'RUSH'
        || !getMetadata(animation)?.rush_combo) {
        delete form.trigger;
    }
    if (!attack && !projectile) {
        if (!soundOnly) {
            delete form.sound_preset;
            delete form.sound_custom;
            delete form.sound_volume;
            delete form.sound_pitch;
        } else {
            delete form.hitbox_enabled;
        }
    }
    if (soundOnly) {
        for (const field of ['offset_x', 'offset_y', 'offset_z', 'size_x', 'size_y', 'size_z',
            'damage_multiplier', 'knockback', 'use_knockback_multiplier', 'knockback_multiplier',
            'repeat_while_rushing', 'effect', 'effect_multiplier', 'effect_duration', 'damage_type',
            'effect_amplifier', 'effect_extra',
            'custom_effect', 'rotation_x', 'rotation_y', 'rotation_z', 'duration_ticks',
            'player_pitch_x_multiplier', 'player_pitch_z_multiplier',
            'horizontal', 'render_pitch',
            'travel_distance', 'particle', 'forward', 'strafe', 'vertical']) {
            delete form[field];
        }
    }

    new Dialog({
        id: `swordskill_timeline_${kind}_event`,
        title: `${existingEntry ? 'Edit' : 'Add'} ${attack ? 'Attack' : projectile ? 'Flying Slash' : soundOnly ? 'Sound' : 'Movement'} Event at ${time.toFixed(3)}s`,
        form,
        onConfirm(values) {
            try {
                const event = attack ? buildAttackEvent(values, tick, data)
                    : projectile ? buildProjectileEvent(values, tick)
                        : soundOnly ? buildSoundEvent(values, tick) : buildMotionEvent(values, tick);
                const keyTime = tick / 20;
                const metadata = getMetadata(animation);
                if (metadata?.legacy_fallback) {
                    // Legacy execution ignores timeline events entirely. Once
                    // the user authors one, switch to the data-driven executor
                    // so the event can affect gameplay as well as the preview.
                    const updatedMetadata = { ...metadata, legacy_fallback: false };
                    const metadataEntry = getMarkers(animation).find(entry =>
                        entry.marker.prefix === SWORDSKILL_META_PREFIX);
                    Undo.initEdit({ animations: [animation] });
                    if (metadataEntry) {
                        updateTimelineEntry(metadataEntry, SWORDSKILL_META_PREFIX, updatedMetadata);
                    }
                    if (existingEntry) {
                        saveEventEntry(animation, existingEntry, keyTime, event);
                        Timeline.vue?.$forceUpdate?.();
                        Undo.finishEdit('Convert legacy skill and edit SwordSkill event', { animations: [animation] });
                        updateSkillPreview(true);
                    } else {
                        addMarker(animation, keyTime, SWORDSKILL_EVENT_PREFIX, event);
                    }
                    Blockbench.showQuickMessage('Switched to data-driven mode. Add motion and attack events for the behavior you want.', 5000);
                    return;
                }
                if (existingEntry) {
                    Undo.initEdit({ animations: [animation] });
                    saveEventEntry(animation, existingEntry, keyTime, event);
                    Timeline.vue?.$forceUpdate?.();
                    Undo.finishEdit('Edit SwordSkill timeline event', { animations: [animation] });
                    updateSkillPreview(true);
                } else {
                    Undo.initEdit({ animations: [animation] });
                    addMarker(animation, keyTime, SWORDSKILL_EVENT_PREFIX, event);
                }
            } catch (error) {
                showError(error);
                return false;
            }
        }
    }).show();
}

function buildSoundEvent(form, tick) {
    const event = {
        kind: 'sound',
        tick,
        sound: String(form.sound_custom || form.sound_preset || '').trim(),
        volume: Number(form.sound_volume),
        pitch: Number(form.sound_pitch)
    };
    if (!/^[a-z0-9_.-]+:[a-z0-9_./-]+$/.test(event.sound)
        || !Number.isFinite(event.volume) || event.volume < 0 || event.volume > 16
        || !Number.isFinite(event.pitch) || event.pitch < 0.5 || event.pitch > 2) {
        throw new Error('Sound ID, volume, or pitch is outside the supported range.');
    }
    return event;
}

function buildAttackEvent(form, tick, previous = {}) {
    const effects = [];
    const type = String(form.effect || 'none');
    if (type === 'custom') {
        const customId = String(form.custom_effect || '').trim();
        if (!customId) throw new Error('Enter the custom effect handler ID registered in Java.');
        // Preserve extra fields of a custom effect that is being re-saved unchanged.
        const original = Array.isArray(previous.effects) ? previous.effects[0] : null;
        const preserved = original && original.type === customId
            ? Object.fromEntries(Object.entries(original).filter(([key]) => key !== 'type'))
            : {};
        effects.push({ ...preserved, type: customId });
    } else if (type === 'stun') {
        effects.push({
            type,
            duration_ticks: Math.max(1, Math.floor(Number(form.effect_duration || 1))),
            amplifier: Math.max(0, Math.floor(Number(form.effect_amplifier || 0)))
        });
    } else if (type !== 'none') {
        effects.push({ type, multiplier: Math.max(0, Number(form.effect_multiplier ?? 1)) });
    }
    const extraRaw = String(form.effect_extra || '').trim();
    if (extraRaw) {
        let extra;
        try {
            extra = JSON.parse(extraRaw);
        } catch (error) {
            throw new Error('Additional effect entries must be a valid JSON array.');
        }
        if (!Array.isArray(extra)) {
            throw new Error('Additional effect entries must be a JSON array.');
        }
        for (const effect of extra) {
            if (!effect || typeof effect !== 'object' || Array.isArray(effect)
                || typeof effect.type !== 'string') {
                throw new Error('Each additional effect entry needs a string "type".');
            }
            effects.push(effect);
        }
    }
    return {
        kind: 'attack',
        tick,
        trigger: String(form.trigger || 'TICK').toUpperCase(),
        hitbox_enabled: Boolean(form.hitbox_enabled),
        offset: [Number(form.offset_x || 0), Number(form.offset_y || 0), Number(form.offset_z || 0)],
        size: [Number(form.size_x), Number(form.size_y), Number(form.size_z)],
        damage_multiplier: Math.max(0, Number(form.damage_multiplier || 0)),
        damage_type: String(form.damage_type || 'BASE'),
        knockback: form.use_knockback_multiplier ? 0 : Number(form.knockback || 0),
        knockback_multiplier: form.use_knockback_multiplier
            ? Number(form.knockback_multiplier || 0) : -1,
        repeat_while_rushing: Boolean(form.repeat_while_rushing),
        effects,
        sound: String(form.sound_custom || form.sound_preset || '').trim()
    };
}

function buildMotionEvent(form, tick) {
    const motion = {
        kind: 'motion',
        tick,
        forward: Number(form.forward || 0),
        strafe: Number(form.strafe || 0),
        vertical: Number(form.vertical || 0)
    };
    if (![motion.forward, motion.strafe, motion.vertical].every(Number.isFinite)) {
        throw new Error('Movement values must be finite numbers.');
    }
    return motion;
}

function buildProjectileEvent(form, tick) {
    const event = {
        kind: 'projectile',
        tick,
        trigger: String(form.trigger || 'TICK').toUpperCase(),
        offset: [Number(form.offset_x || 0), Number(form.offset_y || 0), Number(form.offset_z || 0)],
        rotation: [Number(form.rotation_x || 0), Number(form.rotation_y || 0), Number(form.rotation_z || 0)],
        player_pitch_x_multiplier: Number(form.player_pitch_x_multiplier || 0),
        player_pitch_z_multiplier: Number(form.player_pitch_z_multiplier || 0),
        horizontal: Boolean(form.horizontal),
        render_pitch: Number(form.render_pitch || 0),
        size: [Number(form.size_x), Number(form.size_y), Number(form.size_z)],
        damage_multiplier: Number(form.damage_multiplier),
        damage_type: String(form.damage_type || 'BASE'),
        knockback: form.use_knockback_multiplier ? 0 : Number(form.knockback || 0),
        knockback_multiplier: form.use_knockback_multiplier
            ? Number(form.knockback_multiplier || 0) : -1,
        duration_ticks: Math.floor(Number(form.duration_ticks)),
        travel_distance: Number(form.travel_distance),
        particle: String(form.particle || '').trim(),
        sound: String(form.sound_custom || form.sound_preset || '').trim()
    };
    if (form.use_movement) {
        event.movement = [Number(form.movement_x), Number(form.movement_y), Number(form.movement_z)];
    }
    const numericValues = [
        ...event.offset, ...event.rotation, ...event.size, event.damage_multiplier,
        event.knockback, event.duration_ticks, event.travel_distance,
        event.player_pitch_x_multiplier, event.player_pitch_z_multiplier, event.render_pitch,
        ...(event.movement || [])
    ];
    if (!numericValues.every(Number.isFinite)
        || event.size.some(size => size <= 0)
        || event.damage_multiplier < 0 || !['BASE', 'RUSH'].includes(event.damage_type)
        || Math.abs(event.knockback) > 16
        || event.duration_ticks < 1 || event.duration_ticks > 1200
        || event.travel_distance < 0 || event.travel_distance > 128
        || Math.abs(event.player_pitch_x_multiplier) > 4
        || Math.abs(event.player_pitch_z_multiplier) > 4
        || Math.abs(event.render_pitch) > 180
        || event.movement?.some(value => Math.abs(value) > 128)
        || (event.knockback_multiplier !== -1 && Math.abs(event.knockback_multiplier) > 16)
        || !event.particle || event.particle.length > 128) {
        throw new Error('Flying slash values are outside the supported ranges.');
    }
    return event;
}

function editSelectedTimelineEvent() {
    const animation = currentAnimation();
    if (!animation) return;
    const selectedEntries = getMarkers(animation).filter(entry => entry.keyframe.selected
        && entry.marker.prefix === SWORDSKILL_EVENT_PREFIX);
    if (!selectedEntries.length) {
        Blockbench.showMessageBox({
            title: 'No SwordSkill event selected',
            message: 'Select an attack, flying slash, or movement event keyframe on the Effects > Timeline track first.',
            buttons: ['OK']
        });
        return;
    }
    if (selectedEntries.length === 1) {
        openTimelineEventDialog(selectedEntries[0].marker.data.kind, selectedEntries[0]);
        return;
    }
    const labels = selectedEntries.map((entry, index) => {
        const kind = entry.marker.data.kind === 'projectile' ? 'Flying Slash'
            : entry.marker.data.kind === 'attack' ? 'Hitbox'
                : entry.marker.data.kind === 'sound' ? 'Sound' : 'Motion';
        return `${kind} ${index + 1}`;
    });
    Blockbench.showMessageBox({
        title: 'Choose event to edit',
        message: 'This keyframe contains multiple SwordSkill events.',
        buttons: [...labels, 'Cancel']
    }, result => {
        if (result >= 0 && result < selectedEntries.length) {
            const entry = selectedEntries[result];
            openTimelineEventDialog(entry.marker.data.kind, entry);
        }
    });
}

function importSkillData() {
    const animation = currentAnimation();
    if (!animation) return;
    Blockbench.import({
        resource_id: 'swordskill:skilldata',
        type: 'SwordSkill skilldata',
        extensions: ['json'],
        single_file: true
    }, files => {
        const file = files[0];
        let definition;
        try {
            definition = JSON.parse(file.content);
        } catch (error) {
            showError(new Error('Invalid JSON: ' + error.message));
            return;
        }
        try {
            Undo.initEdit({ animations: [animation] });

            // Write metadata
            const meta = {
                id: definition.id,
                name: definition.name,
                animation: definition.animation,
                replace_builtin: definition.replace_builtin ?? true,
                legacy_fallback: definition.legacy_fallback ?? false,
                type: definition.type || 'SIMPLE',
                weapons: definition.weapons || [],
                cooldown: definition.cooldown ?? 80,
                sp_cost: definition.sp_cost ?? 4,
                duration_ticks: definition.duration_ticks ?? 20,
                transform_limit_ticks: definition.transform_limit_ticks ?? 20,
                hidden: definition.hidden ?? false,
                color: definition.color || '#54D8FF',
                ...(definition.rush_start_ticks != null ? { rush_start_ticks: definition.rush_start_ticks } : {}),
                ...(definition.rush_start_animation ? { rush_start_animation: definition.rush_start_animation } : {}),
                ...(definition.rush_move_animation ? { rush_move_animation: definition.rush_move_animation } : {}),
                ...(definition.rush_finish_animation ? { rush_finish_animation: definition.rush_finish_animation } : {}),
                ...(definition.rush_combo ? { rush_combo: definition.rush_combo } : {}),
                ...(definition.trail_length != null ? { trail_length: definition.trail_length } : {}),
                ...(definition.shockwave_ticks != null ? { shockwave_ticks: definition.shockwave_ticks } : {}),
                ...(definition.hitstop_ticks != null ? { hitstop_ticks: definition.hitstop_ticks } : {})
            };
            // Remove existing meta marker first
            const existingMeta = getMarkers(animation).find(e => e.marker.prefix === SWORDSKILL_META_PREFIX);
            if (existingMeta) {
                effectTimeline(animation, false)?.timeline.remove(existingMeta.keyframe);
            }
            addTimelineRecord(animation, 0, SWORDSKILL_META_PREFIX, meta);

            // Remove all existing event markers
            const existingEvents = getMarkers(animation).filter(e => e.marker.prefix === SWORDSKILL_EVENT_PREFIX);
            const removedKeyframes = new Set();
            for (const entry of existingEvents) {
                if (!removedKeyframes.has(entry.keyframe)) {
                    effectTimeline(animation, false)?.timeline.remove(entry.keyframe);
                    removedKeyframes.add(entry.keyframe);
                }
            }

            // Write hitboxes
            for (const hb of (definition.hitboxes || [])) {
                const time = (hb.tick || 0) / 20;
                addTimelineRecord(animation, time, SWORDSKILL_EVENT_PREFIX, {
                    kind: 'attack',
                    trigger: hb.trigger || 'TICK',
                    hitbox_enabled: hb.hitbox_enabled !== false,
                    offset: hb.offset || [0, 1, 1.5],
                    size: hb.size || [3, 2, 3],
                    damage_multiplier: hb.damage_multiplier ?? 1,
                    damage_type: hb.damage_type || 'BASE',
                    knockback: hb.knockback ?? 1,
                    knockback_multiplier: hb.knockback_multiplier ?? -1,
                    repeat_while_rushing: hb.repeat_while_rushing ?? false,
                    effects: hb.effects || [],
                    sound: hb.sound || ''
                });
            }

            // Write projectiles
            for (const proj of (definition.projectiles || [])) {
                const time = (proj.tick || 0) / 20;
                addTimelineRecord(animation, time, SWORDSKILL_EVENT_PREFIX, {
                    kind: 'projectile',
                    trigger: proj.trigger || 'TICK',
                    offset: proj.offset || [0, 1.2, 2],
                    rotation: proj.rotation || [0, 0, 0],
                    size: proj.size || [4, 3, 1.5],
                    damage_multiplier: proj.damage_multiplier ?? 1,
                    damage_type: proj.damage_type || 'BASE',
                    knockback: proj.knockback ?? 1,
                    knockback_multiplier: proj.knockback_multiplier ?? -1,
                    duration_ticks: proj.duration_ticks ?? 12,
                    travel_distance: proj.travel_distance ?? 16,
                    ...(proj.movement ? { movement: proj.movement } : {}),
                    particle: proj.particle || 'skill_particle_sword/blue',
                    sound: proj.sound || '',
                    horizontal: proj.horizontal ?? false,
                    player_pitch_x_multiplier: proj.player_pitch_x_multiplier ?? 0,
                    player_pitch_z_multiplier: proj.player_pitch_z_multiplier ?? 0,
                    render_pitch: proj.render_pitch ?? 0
                });
            }

            // Write motions
            for (const mot of (definition.motions || [])) {
                const time = (mot.tick || 0) / 20;
                addTimelineRecord(animation, time, SWORDSKILL_EVENT_PREFIX, {
                    kind: 'motion',
                    forward: mot.forward ?? 0,
                    strafe: mot.strafe ?? 0,
                    vertical: mot.vertical ?? 0
                });
            }

            // Write sounds
            for (const snd of (definition.sounds || [])) {
                const time = (snd.tick || 0) / 20;
                addTimelineRecord(animation, time, SWORDSKILL_EVENT_PREFIX, {
                    kind: 'sound',
                    sound: snd.sound || '',
                    volume: snd.volume ?? 1,
                    pitch: snd.pitch ?? 1
                });
            }

            Undo.finishEdit('Import SwordSkill skilldata', { animations: [animation] });
            previewSignature = '';
            timelineMarkerSignature = '';
            updateSkillPreview(true);
            Blockbench.showMessageBox({
                title: 'Import complete',
                message: `Imported skill "${definition.name}" into the animation timeline.`,
                buttons: ['OK']
            });
        } catch (error) {
            Undo.cancelEdit();
            showError(error);
        }
    });
}

function exportSkillData() {

    const animation = currentAnimation();
    if (!animation) return;
    try {
        const metadata = getMetadata(animation);
        if (!metadata) throw new Error('Set skill metadata on this animation first.');
        const entries = getMarkers(animation)
            .filter(entry => entry.marker.prefix === SWORDSKILL_EVENT_PREFIX)
            .map(entry => ({ ...entry.marker.data, tick: Math.round(entry.time * 20) }));
        const hitboxes = entries.filter(event => event.kind === 'attack').map(event => ({
            tick: event.tick,
            ...(event.trigger && event.trigger !== 'TICK' ? { trigger: event.trigger } : {}),
            hitbox_enabled: event.hitbox_enabled !== false,
            offset: event.offset,
            size: event.size,
            damage_multiplier: event.damage_multiplier,
            damage_type: event.damage_type || 'BASE',
            knockback: event.knockback,
            knockback_multiplier: event.knockback_multiplier ?? -1,
            repeat_while_rushing: Boolean(event.repeat_while_rushing),
            effects: event.effects || [],
            sound: String(event.sound || '').trim()
        }));
        const motions = entries.filter(event => event.kind === 'motion').map(event => ({
            tick: event.tick,
            forward: event.forward,
            strafe: event.strafe,
            vertical: event.vertical
        }));
        const projectiles = entries.filter(event => event.kind === 'projectile').map(event => ({
            tick: event.tick,
            ...(event.trigger && event.trigger !== 'TICK' ? { trigger: event.trigger } : {}),
            offset: event.offset,
            rotation: event.rotation,
            ...(event.player_pitch_x_multiplier
                ? { player_pitch_x_multiplier: event.player_pitch_x_multiplier } : {}),
            ...(event.player_pitch_z_multiplier
                ? { player_pitch_z_multiplier: event.player_pitch_z_multiplier } : {}),
            ...(event.horizontal ? { horizontal: true } : {}),
            ...(event.render_pitch ? { render_pitch: event.render_pitch } : {}),
            size: event.size,
            damage_multiplier: event.damage_multiplier,
            damage_type: event.damage_type || 'BASE',
            knockback: event.knockback,
            knockback_multiplier: event.knockback_multiplier ?? -1,
            duration_ticks: event.duration_ticks,
            travel_distance: event.travel_distance,
            ...(event.movement ? { movement: event.movement } : {}),
            particle: event.particle,
            sound: event.sound
        }));
        const sounds = entries.filter(event => event.kind === 'sound').map(event => ({
            tick: event.tick,
            sound: event.sound,
            volume: event.volume,
            pitch: event.pitch
        }));
        const lastEventTick = entries.reduce((latest, event) => Math.max(latest, event.tick), 0);
        const durationTicks = Math.max(Number(metadata.duration_ticks
            ?? Math.round(Number(animation.length || 0) * 20)), lastEventTick, 1);
        if (!Number.isSafeInteger(durationTicks) || durationTicks > 1200) {
            throw new Error('Skill duration must be a whole number from 1 to 1200 ticks.');
        }
        if (metadata.type === 'RUSH' && metadata.rush_start_ticks > durationTicks) {
            throw new Error('Rush startup duration cannot exceed skill duration.');
        }

        const definition = {
            schema_version: 1,
            id: metadata.id ?? 0,
            replace_builtin: Boolean(metadata.replace_builtin),
            legacy_fallback: Boolean(metadata.legacy_fallback),
            name: metadata.name,
            animation: metadata.animation,
            rush_start_ticks: metadata.rush_start_ticks,
            rush_start_animation: metadata.rush_start_animation,
            rush_move_animation: metadata.rush_move_animation,
            rush_finish_animation: metadata.rush_finish_animation,
            ...(metadata.rush_combo ? { rush_combo: metadata.rush_combo } : {}),
            type: metadata.type,
            weapons: metadata.weapons,
            cooldown: metadata.cooldown,
            sp_cost: metadata.sp_cost,
            duration_ticks: durationTicks,
            transform_limit_ticks: metadata.transform_limit_ticks,
            hidden: metadata.hidden,
            color: metadata.color,
            trail_length: metadata.trail_length ?? 1.5,
            shockwave_ticks: metadata.shockwave_ticks ?? 0,
            hitstop_ticks: metadata.hitstop_ticks ?? -1,
            motions,
            hitboxes,
            projectiles,
            sounds
        };
        Blockbench.export({
            resource_id: 'swordskill:skilldata',
            type: 'SwordSkill skilldata',
            extensions: ['json'],
            name: `${metadata.name}.json`,
            content: JSON.stringify(definition, null, 2) + '\n'
        });
    } catch (error) {
        showError(error);
    }
}

function showError(error) {
    Blockbench.showMessageBox({
        title: 'SwordSkill Timeline',
        message: error.message || String(error),
        buttons: ['OK']
    });
}
