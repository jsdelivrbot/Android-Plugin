// game logic
var deviceID = null;
var players = {};
var lasers = [], charges = [];
var now, oob, screenshake;

var firing = false;
var shot = null;
var charged = null;
var charging = null;

var background = new THREE.Color(0x000000);
var motionDnaReady = false;

var LEFT = -24.7,
    RIGHT = 24.7,
    TOP = -24.7,
    BOTTOM = 24.7;
var STATIONARY = 0,
    FIDGETING = 1,
    FORWARD = 2;

var COOLDOWN = 125;
var LASER_SPEED = 16;
var LASER_DELAY = 500;
var LASER_CHARGE = 2500;
var CHARGE_THICK = 5 / 2;
var CHARGE_THICK2 = CHARGE_THICK * CHARGE_THICK;
var CHARGE_LENGTH = 25 / 2;
var CHARGE_LENGTH2 = CHARGE_LENGTH + CHARGE_LENGTH;
var CHARGE_LIFESPAN = 5000;
var OUT_OF_BOUNDS = 3000;
var SCREEN_SHAKE = 1000;
var CAMERA_SHAKINESS = 0.5;

var BLACK = new THREE.Color(0x000000), RED = new THREE.Color(0xff0000), WHITE = new THREE.Color(0xffffff);
var BRIGHTNESS = 10;

// renderer

var camera, scene, renderer, stats, clock, raycaster;
var geometry, chargeExplosion, starsMaterials, starsColors = [0x444444, 0x888888, 0xcccccc];
var timeDelta;

var CAMERA_HEIGHT = 50;

init();
animate();

function init() {
    // renderer

    var aspect = window.innerWidth / window.innerHeight;
    camera = new THREE.PerspectiveCamera(10, window.innerWidth / window.innerHeight, 1, 1000);
    camera.position.y = 500;

    scene = new THREE.Scene();
    camera.lookAt(scene.position);

    // grid

    var gridHelper = new THREE.GridHelper(50, 10, "#888888", "#111111");
    gridHelper.position.set(0, -0.125, 0);
    scene.add(gridHelper);
    var bounds = new THREE.GridHelper(50, 2, "#888888", "#ffffff");
    bounds.position.set(0, -0.1, 0);
    scene.add(bounds);

    // geometries

    geometry = new THREE.Geometry;
    geometry.vertices.push(new THREE.Vector3(0, 0.1, 0));
    geometry.vertices.push(new THREE.Vector3(0.433, -0.1, -0.25));
    geometry.vertices.push(new THREE.Vector3(0, -0.1, -0.125));
    geometry.vertices.push(new THREE.Vector3(-0.433, -0.1, -0.25));
    geometry.vertices.push(new THREE.Vector3(0, -0.1, 0.25));
    geometry.faces.push(new THREE.Face3(0, 1, 2));
    geometry.faces.push(new THREE.Face3(0, 2, 3));
    geometry.faces.push(new THREE.Face3(0, 3, 4));
    geometry.faces.push(new THREE.Face3(0, 4, 1));
    geometry.computeFaceNormals();

    // stars

    starsMaterials = [new THREE.PointsMaterial({
            color: starsColors[0]
        }),
        new THREE.PointsMaterial({
            color: starsColors[1]
        }),
        new THREE.PointsMaterial({
            color: starsColors[2]
        })
    ];

    for (var j = 0; j < 5; j++) {
        var starsGeometry = new THREE.Geometry();

        for (var i = 0; i < 1000; i++) {
            var star = new THREE.Vector3();
            star.x = THREE.Math.randFloatSpread(100);
            star.y = THREE.Math.randFloatSpread(250) - 100;
            star.z = THREE.Math.randFloatSpread(100);

            starsGeometry.vertices.push(star);
        }

        var starsMaterial = starsMaterials[j % 3];

        var starField = new THREE.Points(starsGeometry, starsMaterial);

        scene.add(starField);
    }

    // Lights

    scene.add(new THREE.AmbientLight(0x222222));

    var light = new THREE.PointLight(0xffffff, 0.5, 100);
    light.position.set(0, 5, 0);
    scene.add(light);

    var directionalLight = new THREE.DirectionalLight( 0xffffff, 0.5 );
    directionalLight.position.set(1, 1, 1);
    scene.add(directionalLight);

    chargeExplosion = new THREE.DirectionalLight( 0xffffff, 0 );
    scene.add(chargeExplosion);

    // renderer

    renderer = new THREE.WebGLRenderer();
    renderer.setPixelRatio(window.devicePixelRatio);
    renderer.setSize(window.innerWidth, window.innerHeight);
    document.body.appendChild(renderer.domElement);

    stats = new Stats();
    document.body.appendChild(stats.dom);

    clock = new THREE.Clock();

    raycaster = new THREE.Raycaster();
    raycaster.near = 0;
}

function animate() {
    requestAnimationFrame(animate);

    stats.begin();
    timeDelta = clock.getDelta();
    render();
    stats.end();
}

function render() {
    now = new Date().getTime();

    if (!motionDnaReady) {
        camera.rotation.z = now / 5000;
    }

    var player = null;
    var other = null;
    if (motionDnaReady && deviceID != null && deviceID in players) {
        player = players[deviceID];
        camera.position.x = player.x;
        if (camera.position.y > CAMERA_HEIGHT) {
            camera.position.y += timeDelta * (CAMERA_HEIGHT - camera.position.y);
            if (camera.position.y - CAMERA_HEIGHT < 0.1) {
                camera.position.y = CAMERA_HEIGHT;
            }
        }
        camera.position.z = player.y;
        camera.rotation.z = (player.h + 180) * Math.PI / 180;

        if (firing && now - shot > LASER_DELAY) {
            shot = now;
            window.JSInterface.shot();
        }

        if (firing && charged == null && charging != null && now - charging > 1.25 * LASER_DELAY) {
            charged = new THREE.Points(geometry, new THREE.PointsMaterial({transparent: true, opacity: 0}));
            charged.alive = charging;
            charged.vibrated = false;
            charged.position.y = 5;
            var light = new THREE.PointLight(player.color, 0, 10);
            charged.add(light);
            charged.light = light;
            player.mesh.add(charged);
        }

        if (charged != null) {
            if (charging != null) {
                if (now - charging > LASER_CHARGE) {
                    charged.material.opacity = 1;
                    charged.light.intensity = 2.5;
                    if (!charged.vibrated) {
                        charged.vibrated = true;
                        window.JSInterface.vibrated(100);
                    }
                } else {
                    charged.material.opacity += 1000 * timeDelta / 1.25 / LASER_CHARGE * (0.5 - charged.material.opacity);
                    charged.light.intensity += 1000 * timeDelta / 1.25 / LASER_CHARGE * (1 - charged.light.intensity);
                    charged.material.opacity = Math.min(charged.material.opacity, 0.5);
                    charged.light.intensity = Math.min(charged.light.intensity, 1);
                    charged.vibrated = false;
                }
            } else {
                charged.material.opacity += 1000 * timeDelta / LASER_CHARGE * (0 - charged.material.opacity);
                charged.light.intensity += 1000 * timeDelta / LASER_CHARGE * (0 - charged.light.intensity);
                charged.vibrated = false;
            }
        }

        if (screenshake) {
            if (now - screenshake > SCREEN_SHAKE) {
                scene.background = screenshake = null;
                background.copy(BLACK);
                chargeExplosion.intensity = 0;
            } else {
                scene.background = background.lerp(BLACK, (now - screenshake) * (now - screenshake) / SCREEN_SHAKE / SCREEN_SHAKE);
                var color = {};
                scene.background.getHSL(color);
                chargeExplosion.intensity = BRIGHTNESS * color.l;

                var shakiness = 0.5 - Math.abs((now - screenshake) / SCREEN_SHAKE - 0.5);
                shakiness *= shakiness;
                camera.position.x += CAMERA_SHAKINESS * Math.max(color.l, shakiness) * Math.sin(now / 12.5);
                camera.position.z += CAMERA_SHAKINESS * Math.max(color.l, shakiness) * Math.cos(now / 12.5);
            }
        } else if (oob) {
            if (now - oob > OUT_OF_BOUNDS) {
                SET_ID(deviceID);
                scene.background = oob = null;
                background.copy(BLACK);
            } else {
                scene.background = background.lerp(RED, timeDelta * (now - oob) / OUT_OF_BOUNDS);
            }
        }

        for (var id in players) {
            other = players[id];
            if (other.label) {
                var s = Math.sin(player.h * Math.PI / 180);
                var c = Math.cos(player.h * Math.PI / 180);
                other.label.position.set(other.x - 0.5 * s + (1 - other.label.width) * c,
                                         1,
                                         other.y - 0.5 * c - (1 - other.label.width) * s);
            }
        }

        raycaster.far = 2 * timeDelta * LASER_SPEED;
    }

    // player = other;

    for (var i = lasers.length - 1; i >= 0; i--) {
        var laser = lasers[i];
        // var collided = false;

        if (player != null && !laser.collided) {
            raycaster.set(laser.position, laser.direction);
            var intersects = raycaster.intersectObject(player.mesh);
            if (intersects.length > 0) {
                laser.collided = true;
                window.JSInterface.vibrated(250);
            }
        }

        laser.position.add(laser.direction.clone().multiplyScalar(timeDelta * laser.speed));
        if (laser.speed != LASER_SPEED) laser.speed = LASER_SPEED;
        if (laser.position.x < 2 * LEFT || laser.position.x > 2 * RIGHT ||
            laser.position.z < 2 * TOP || laser.position.z > 2 * BOTTOM) {
            scene.remove(laser);
            lasers.splice(i, 1);
        }
    }

    for (var i = charges.length - 1; i >= 0; i--) {
        var charge = charges[i];

        if (player != null && !charge.collided && now - charge.alive < 1.25 * CHARGE_LIFESPAN) {
            charge.line.start = charge.object3d.position;
            charge.line.end.copy(charge.line.start);
            charge.line.end.add(charge.object3d.direction.clone().multiplyScalar(CHARGE_LENGTH2));
            var lengthAt = charge.line.closestPointToPointParameter(player.mesh.position, false);
            if (lengthAt >= 0 && lengthAt <= 1) {
                charge.line.at(lengthAt, charge.closest);
                var width2 = charge.closest.distanceToSquared(player.mesh.position);
                lengthAt -= 0.5;
                if (width2 <= CHARGE_THICK2 &&
                    width2 / CHARGE_THICK2 + 4 * lengthAt * lengthAt <= 1) {
                    charge.collided = true;
                    window.JSInterface.vibrated(1000);
                }
            }
        }

        charge.object3d.position.add(charge.object3d.direction.clone().multiplyScalar(timeDelta * charge.object3d.speed));
        charge.object3d.speed = (1 + timeDelta) * charge.object3d.speed + timeDelta;

        if (now - charge.alive > CHARGE_LIFESPAN) {
            if (now - charge.alive > 2 * CHARGE_LIFESPAN) {
                scene.remove(charge.object3d);
                charges.splice(i, 1);
            } else {
                charge.material.color = charge.material.color.lerp(BLACK, (now - charge.alive - CHARGE_LIFESPAN) / CHARGE_LIFESPAN);
            }
        }
    }

    for (var i = starsMaterials.length - 1; i >= 0; i--) {
        var oscillate = Math.sin(now / 125 + 2.1 * i);
        var starsMaterial = starsMaterials[i];
        starsMaterial.color = new THREE.Color(starsColors[i]).addScalar(0.05 * oscillate);
    }

    renderer.render(scene, camera);
}

function makePlayer(player) {
    var material = new THREE.MeshLambertMaterial({
        color: player.color
    });

    var mesh = new THREE.Mesh(geometry, material);
    scene.add(mesh);

    var light = new THREE.PointLight(player.color, 2, 50, 2);
    mesh.add(light);

    var label = TextLabel(player.name);
    scene.add(label);

    player.label = label;
    player.mesh = mesh;

    label.position.x = label.position.z = mesh.position.x = mesh.position.z = 1000;
}

function unmakePlayer(player) {
    scene.remove(player.label);
    scene.remove(player.mesh);
}

function uncharge() {
    if (charged != null) {
        var player = players[deviceID];
        if (player) player.mesh.remove(charged);
    }
    charged = charging = null;
}

// events

window.addEventListener("touchstart", function() {
    firing = true;
});

window.addEventListener("touchend", function() {
    firing = false;
    if (charging != null && now - charging > LASER_CHARGE) {
        window.JSInterface.charged();
        uncharge();
    }
    charging = null;
    if (shot != null) {
        shot -= COOLDOWN;
    }
});

// API

function SET_ID(id) {
    deviceID = id;
    if (!(id in players)) META(id, null, null);
    var player = players[id];
    player.x = THREE.Math.randFloatSpread(45);
    player.y = THREE.Math.randFloatSpread(45);
    player.h = THREE.Math.randFloatSpread(180) + 180;
    player.vibrated = false;
    window.JSInterface.position(player.x, player.y, player.h);
}

function META(id, name, hex) {
    if (name == null) {
        name = id;
    }
    if (hex == null) {
        hex = "#" + id.substring(0, 6);
    }
    if (!(id in players)) {
        players[id] = {};
    } else {
        unmakePlayer(players[id]);
    }
    players[id].name = name;
    players[id].color = new THREE.Color(hex) || new THREE.Color();
    var color = {};
    players[id].color.getHSL(color);
    players[id].color.setHSL(color.h, color.s, color.l / 2 + 0.5);
    makePlayer(players[id]);
}

function MOVE(id, x, y, h, mode) {
    if (!(id in players)) META(id, null, null);
    var player = players[id];
    if (!motionDnaReady && id == deviceID) {
        motionDnaReady = true;
    }

    var wall = false;
    player.x = x;
    player.y = y;
    if (x < LEFT) {
        wall = true;
        player.x = LEFT;
    }
    if (x > RIGHT) {
        wall = true;
        player.x = RIGHT;
    }
    if (y < TOP) {
        wall = true;
        player.y = TOP;
    }
    if (y > BOTTOM) {
        wall = true;
        player.y = BOTTOM;
    }
    player.h = 90 - h;

    player.mesh.position.x = player.x;
    player.mesh.position.z = player.y;
    player.mesh.rotation.y = player.h * Math.PI / 180;

    if (id == deviceID) {
        if (wall) {
            if (!oob) oob = new Date().getTime();
        } else if (oob) {
            oob += 250;
            scene.background = background.lerp(BLACK, 0.25);
            if (oob > now) {
                scene.background = oob = null;
            }
        }

        charging = (!firing || mode == FORWARD) ? null : (charging || new Date().getTime());
    }
}

function SHOOT(id) {
    if (!(id in players)) return;
    var player = players[id];

    var color = {};
    player.color.getHSL(color);
    var laser = new LaserBeam(new THREE.Color().setHSL(color.h, 0.25, 0.25)).object3d;
    laser.direction = new THREE.Vector3(Math.sin(player.h * Math.PI / 180), 0, Math.cos(player.h * Math.PI / 180));
    laser.lookAt(laser.direction);
    laser.rotateY(-Math.PI / 2);
    laser.position.x = player.x;
    laser.position.z = player.y;
    laser.speed = 0;
    lasers.push(laser);
    scene.add(laser);

    if (id == deviceID) {
        laser.collided = true;
    } else {
        laser.collided = false;
    }
}

function CHARGE(id) {
    if (!(id in players)) return;
    var player = players[id];

    var color = {};
    player.color.getHSL(color);
    var charge = new ChargeBeam(new THREE.Color().setHSL(color.h, 0.25, 0.25));
    charges.push(charge);
    charge.alive = new Date().getTime();
    charge.collided = false;
    charge = charge.object3d;
    charge.direction = new THREE.Vector3(Math.sin(player.h * Math.PI / 180), 0, Math.cos(player.h * Math.PI / 180));
    charge.lookAt(charge.direction);
    charge.rotateY(-Math.PI / 2);
    charge.position.x = player.x;
    charge.position.z = player.y;
    charge.speed = 0;
    scene.add(charge);

    screenshake = new Date().getTime();
    if (id != deviceID) {
        screenshake -= SCREEN_SHAKE / 2;
        scene.background = background.copy(WHITE).addScalar(-0.5);
    } else {
        scene.background = background.copy(WHITE);
    }
    return charge;
}

function EXIT(id) {
    if (id in players) {
        scene.remove(players[id].mesh);
        delete players[id];
        if (deviceID == id) {
            deviceID = null;
        }
    }
}

// imports

var laserCanvas, chargeCanvas;

LaserBeam = function(c) {
    var object3d = new THREE.Object3D()
    this.object3d = object3d
    // generate the texture
    var canvas = laserCanvas || generateLaserBodyCanvas()
    var texture = new THREE.Texture(canvas)
    texture.needsUpdate = true;
    // do the material
    var material = new THREE.MeshBasicMaterial({
        map: texture,
        blending: THREE.AdditiveBlending,
        color: c,
        side: THREE.DoubleSide,
        depthWrite: false,
        transparent: true
    })
    var geometry = new THREE.PlaneGeometry(0.5, 0.05)
    var nPlanes = 16;
    for (var i = 0; i < nPlanes; i++) {
        var mesh = new THREE.Mesh(geometry, material)
        mesh.position.x = 0.5 / 2 + THREE.Math.randFloatSpread(0.05);
        mesh.rotation.x = i / nPlanes * Math.PI
        object3d.add(mesh)
    }
    return

    function generateLaserBodyCanvas() {
        // init canvas
        var canvas = document.createElement('canvas');
        var context = canvas.getContext('2d');
        canvas.width = 1;
        canvas.height = 64;
        // set gradient
        var gradient = context.createLinearGradient(0, 0, canvas.width, canvas.height);
        gradient.addColorStop(0, 'rgba( 0, 0, 0,0.1)');
        gradient.addColorStop(0.1, 'rgba(160,160,160,0.3)');
        gradient.addColorStop(0.5, 'rgba(255,255,255,0.5)');
        gradient.addColorStop(0.9, 'rgba(160,160,160,0.3)');
        gradient.addColorStop(1.0, 'rgba( 0, 0, 0,0.1)');
        // fill the rectangle
        context.fillStyle = gradient;
        context.fillRect(0, 0, canvas.width, canvas.height);
        // return the just built canvas
        laserCanvas = canvas;
        return canvas;
    }
}

ChargeBeam = function(c) {
    var object3d = new THREE.Object3D()
    this.object3d = object3d
    // generate the texture
    var canvas = chargeCanvas || generateChargeBodyCanvas()
    var texture = new THREE.Texture(canvas)
    texture.needsUpdate = true;
    // do the material
    var material = new THREE.MeshBasicMaterial({
        map: texture,
        blending: THREE.AdditiveBlending,
        color: c,
        side: THREE.DoubleSide,
        depthWrite: false,
        transparent: true
    })
    this.material = material
    var geometry = new THREE.PlaneGeometry(2 * CHARGE_LENGTH, 2 * CHARGE_THICK)
    var nPlanes = 24;
    for (var i = 0; i < nPlanes; i++) {
        var mesh = new THREE.Mesh(geometry, material)
        mesh.position.x = CHARGE_LENGTH + THREE.Math.randFloatSpread(CHARGE_THICK / 5);
        mesh.rotation.x = i / nPlanes * Math.PI
        object3d.add(mesh)
    }
    var line = new THREE.Line3();
    this.line = line;
    var closest = new THREE.Vector3();
    this.closest = closest;
    return

    function generateChargeBodyCanvas() {
        // init canvas
        var canvas = document.createElement('canvas');
        var context = canvas.getContext('2d');
        canvas.width = 256;
        canvas.height = 256;
        // set gradient
        var gradient = context.createRadialGradient(canvas.width / 2, canvas.height / 2, 128,
                                                    canvas.width / 2, canvas.height / 2, 0);
        gradient.addColorStop(0, 'rgba( 0, 0, 0,0.1)');
        gradient.addColorStop(0.1, 'rgba(160,160,160,0.5)');
        gradient.addColorStop(1, 'rgba(255,255,255,1)');
        // fill the rectangle
        context.fillStyle = gradient;
        context.fillRect(0, 0, canvas.width, canvas.height);
        // return the just built canvas
        chargeCanvas = canvas;
        return canvas;
    }
}

TextLabel = function(message) {
	var fontsize = 32;

	var canvas = document.createElement('canvas');
	var context = canvas.getContext('2d');
    canvas.width = 512;
    canvas.height = 64;
	context.font = "Bold " + fontsize + "px Arial";

	var metrics = context.measureText( message );
	var textWidth = metrics.width;

	context.fillStyle = "rgba(255, 255, 255, 0.5)";
	context.fillRect(canvas.width - textWidth - 5, 0, textWidth + 10, fontsize * 1.2 + 2);

	context.fillStyle = "#ffffff";
	context.fillText( message, canvas.width - textWidth, fontsize + 1 );

	// canvas contents will be used for a texture
	var texture = new THREE.Texture(canvas)
	texture.needsUpdate = true;

	var spriteMaterial = new THREE.SpriteMaterial({ map: texture });
	var sprite = new THREE.Sprite( spriteMaterial );
	sprite.scale.set(2, 0.25, 1);

	sprite.width = textWidth / canvas.width;
	return sprite;
}
