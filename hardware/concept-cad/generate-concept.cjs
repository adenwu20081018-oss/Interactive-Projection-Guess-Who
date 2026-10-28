// Documentation-only generator. Node.js; no application/firmware dependencies.
// Generates an editable OpenSCAD file and independent SVG projections from one scene.
const fs = require('fs');
const path = require('path');
const dir = __dirname;
const C={desk:'#bca38b',stand:'#536477',camera:'#33a9bc',projector:'#8569bd',support:'#b8bec6',laptop:'#476fb0',mega:'#369c7b',area:'#e1bd49',beam:'#ac82ce',view:'#28a7b9'};
const scene=[];
const add=(name,color,points,faces)=>scene.push({name,color,points,faces});
function box(name,c,x,y,z,w,d,h){
 const p=[[x,y,z],[x+w,y,z],[x+w,y+d,z],[x,y+d,z],[x,y,z+h],[x+w,y,z+h],[x+w,y+d,z+h],[x,y+d,z+h]];
 add(name,c,p,[[0,3,2,1],[4,5,6,7],[0,1,5,4],[1,2,6,5],[2,3,7,6],[3,0,4,7]]);
}
const sub=(a,b)=>a.map((v,i)=>v-b[i]);
const dot=(a,b)=>a.reduce((s,v,i)=>s+v*b[i],0);
const cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];
const unit=a=>a.map(v=>v/Math.hypot(...a));
function rod(name,c,a,b,r){
 const axis=unit(sub(b,a)), u=unit(cross(axis,Math.abs(axis[2])>.9?[0,1,0]:[0,0,1])), v=cross(axis,u), p=[];
 for(const end of [a,b])for(let i=0;i<8;i++)p.push(end.map((n,j)=>n+r*(u[j]*Math.cos(i*Math.PI/4)+v[j]*Math.sin(i*Math.PI/4))));
 const faces=[[7,6,5,4,3,2,1,0],[8,9,10,11,12,13,14,15]];
 for(let i=0;i<8;i++)faces.push([i,(i+1)%8,(i+1)%8+8,i+8]);
 add(name,c,p,faces);
}
// Existing ordinary desk, not a tabletop fabrication design. Millimetres.
box('Existing desk placeholder',C.desk,-800,-450,720,1600,900,30);
for(const x of [-740,680])for(const y of [-390,330])box('Existing desk leg',C.desk,x,y,0,60,60,720);
box('Projected interaction area',C.area,-300,-180,751,600,360,2);
// A few visual card tiles; deliberately not an exact game layout.
for(let row=0;row<3;row++)for(let col=0;col<6;col++)box('Illustrative projected card', '#f8edc4',-280+col*96,-160+row*110,753,76,85,1);
// Generic commercially supplied overhead camera stand; no mount interface modeled.
rod('Standard stand mast',C.stand,[0,650,80],[0,650,1350],18);
for(const end of [[-320,850,20],[320,850,20],[0,340,20]])rod('Standard tripod leg',C.stand,[0,650,380],end,14);
rod('Existing overhead stand arm',C.stand,[0,650,1350],[0,170,1350],14);
rod('Concept camera connection',C.stand,[0,170,1350],[0,170,1280],10);
box('USB webcam placeholder',C.camera,-45,135,1230,90,70,50);
box('Webcam lens marker', '#173846',-17,135,1220,34,34,10);
// Portable projector on a generic existing stable support, not a custom bracket.
box('Existing stable support top',C.support,-240,-980,950,480,420,30);
for(const x of [-205,155])for(const y of [-945,-645])box('Existing support leg',C.support,x,y,0,50,50,950);
box('Portable projector placeholder',C.projector,-150,-890,980,300,220,90);
box('Projector lens marker','#312541',-25,-672,1005,50,8,40);
box('Laptop base',C.laptop,-710,-150,751,320,220,18);
box('Laptop display placeholder',C.laptop,-710,60,769,320,12,210);
box('Laptop screen', '#d7e6f6',-695,57,784,290,3,180);
box('Mega 2560 envelope',C.mega,490,-55,751,102,54,18);
box('Mega USB connector marker','#cbd2d8',490,-46,769,15,12,10);
// Direction annotations, not optical frusta or verified projection footprints.
const rays=[
 {name:'Camera viewing direction',color:C.view,a:[0,150,1220],b:[0,0,760]},
 {name:'Projector beam direction',color:C.beam,a:[0,-664,1025],b:[0,-30,760]}
];
let scad='// Concept CAD — Not Built or Validated\n// Units: approximate mm. Presentation only; no manufacturing or mounting data.\n// Generated with generate-concept.cjs; editable independently in OpenSCAD.\n// Existing desk, standard overhead stand, existing projector support.\n// No cables, physical interfaces, optical calibration, loads or clearances validated.\nshow_directions = true;\nshow_interaction_tiles = true;\n';
for(const s of scene){
 const rgb=s.color.match(/\w\w/g).map(v=>parseInt(v,16)/255);
 scad+=`\n// ${s.name}\n${s.name==='Illustrative projected card'?'if (show_interaction_tiles) ':''}color(${JSON.stringify(rgb)}) polyhedron(points=${JSON.stringify(s.points)}, faces=${JSON.stringify(s.faces.map(f=>[...f].reverse()))}, convexity=10);\n`;
}
scad+='\nmodule direction(a,b,c) { v=b-a; l=norm(v); color(c) translate(a) rotate(a=acos(v[2]/l),v=[-v[1],v[0],0]) { cylinder(h=l-40,r=5,$fn=16); translate([0,0,l-40]) cylinder(h=40,r1=18,r2=0,$fn=16); } }\n';
for(const r of rays)scad+=`if(show_directions) direction(${JSON.stringify(r.a)},${JSON.stringify(r.b)},${JSON.stringify(r.color.match(/\w\w/g).map(v=>parseInt(v,16)/255))}); // ${r.name}: endpoint marker is at desk\n`;
fs.writeFileSync(path.join(dir,'guess-who-concept.scad'),scad);
fs.writeFileSync(path.join(dir,'scene-reference.json'),JSON.stringify({units:'approximate mm',status:'Concept CAD — Not Built or Validated',scene,rays},null,2));
const views={isometric:[1,-1,0.8],front:[0,-1,0],side:[1,0,0],top:[0,0,1]};
const esc=s=>s.replaceAll('&','&amp;').replaceAll('<','&lt;');
for(const [name,eye] of Object.entries(views)){
 const forward=unit(eye), right=name==='top'?[1,0,0]:unit(cross([0,0,1],forward)), up=cross(forward,right);
 const proj=p=>[dot(p,right),-dot(p,up),dot(p,forward)];
 const all=scene.flatMap(s=>s.points.map(proj));
 const minX=Math.min(...all.map(p=>p[0])),maxX=Math.max(...all.map(p=>p[0])),minY=Math.min(...all.map(p=>p[1])),maxY=Math.max(...all.map(p=>p[1]));
 const scale=Math.min(940/(maxX-minX),680/(maxY-minY));
 const xy=p=>{const a=proj(p);return [70+(940-(maxX-minX)*scale)/2+(a[0]-minX)*scale,170+(680-(maxY-minY)*scale)/2+(a[1]-minY)*scale];};
 const polygons=[];
 for(const s of scene)for(const f of s.faces){
  const ps=f.map(i=>s.points[i]),normal=cross(sub(ps[1],ps[0]),sub(ps[2],ps[0]));
  if(dot(normal,forward)<=0)continue;
  const layer = s.name==='Existing desk placeholder'?1 : s.name==='Existing stable support top'?2 : s.name.includes('leg')||s.name.includes('mast')?0 : s.name==='Projected interaction area'?3 : s.name==='Illustrative projected card'?4 : s.name.includes('stand arm')||s.name.includes('camera')||s.name.includes('Webcam')||s.name.includes('webcam')?6 :5;
  polygons.push({ps,color:s.color,layer,depth:ps.reduce((sum,p)=>sum+proj(p)[2],0)/ps.length});
 }
 polygons.sort((a,b)=>a.layer-b.layer || a.depth-b.depth);
 let svg=`<svg xmlns="http://www.w3.org/2000/svg" width="1440" height="1040" viewBox="0 0 1440 1040"><defs><marker id="arrow" markerWidth="9" markerHeight="9" refX="8" refY="4.5" orient="auto"><path d="M0 0 L9 4.5 L0 9Z" fill="context-stroke"/></marker></defs><rect width="1440" height="1040" fill="#f7f9fc"/><style>text{font-family:Arial,sans-serif;fill:#203247;font-size:19px}.title{font-size:32px;font-weight:bold}.small{font-size:16px}</style><text x="48" y="55" class="title">Guess Who • ${name[0].toUpperCase()+name.slice(1)} concept view</text><text x="48" y="93">Concept CAD — Not Built or Validated</text><text x="48" y="125" class="small">Approximate placeholders • Existing furniture and standard equipment • Documentation only</text>`;
 for(const p of polygons)svg+=`<polygon points="${p.ps.map(v=>xy(v).join(',')).join(' ')}" fill="${p.color}" stroke="${p.cell?p.color:'#405268'}" stroke-width="0.8"/>`;
 for(const r of rays){const a=xy(r.a),b=xy(r.b);svg+=`<path d="M${a} L${b}" fill="none" stroke="${r.color}" stroke-width="4" stroke-dasharray="10 6" marker-end="url(#arrow)"/>`;}
 svg+='<rect x="1060" y="170" width="340" height="650" rx="14" fill="#fff" stroke="#d5dde7"/><text x="1080" y="207" style="font-weight:bold">Component key</text>';
 const legend=[['desk','Ordinary desk'],['stand','Standard overhead stand'],['camera','USB webcam'],['projector','Portable HDMI projector'],['support','Existing stable support'],['laptop','Laptop'],['mega','ELEGOO Mega envelope'],['area','Projected interaction area']];
 legend.forEach(([c,t],i)=>svg+=`<rect x="1080" y="${235+i*44}" width="18" height="18" rx="3" fill="${C[c]}"/><text x="1110" y="${251+i*44}" class="small">${t}</text>`);
 svg+=`<text x="1080" y="630" class="small" style="fill:${C.view}">Dashed teal: camera → desk</text><text x="1080" y="665" class="small" style="fill:${C.beam}">Dashed purple: projector → desk</text><text x="1080" y="715" class="small">Direction lines overlay geometry.</text><text x="1080" y="745" class="small">They are not optical ray traces.</text><text x="1080" y="775" class="small">Hidden parts may be occluded.</text>`;
 svg+='<text x="48" y="915" class="small">Interaction patch: illustrative 600 × 360 mm; actual throw distance, focus, field of view and calibration remain unverified.</text><text x="48" y="950" class="small">No custom frame, printed parts, mounting hardware or new tabletop designed. Cable routes omitted for clarity.</text><text x="48" y="985" class="small">Independent orthographic preview from shared geometry data — not an OpenSCAD-rendered or validated assembly.</text></svg>';
 fs.writeFileSync(path.join(dir,`${name}.svg`),svg);
}
console.log('Generated OpenSCAD model, reference scene and four SVG projections. No native CAD validation performed.');
