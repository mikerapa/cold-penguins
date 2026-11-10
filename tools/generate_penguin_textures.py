"""Paint code-native cube UVs to match the supplied adult/chick concept references.

Adults and chicks use separate 64x64 layouts from PenguinRenderer.Model.
No source-image pixels are used: these are deterministic, editable Minecraft assets.
"""
from pathlib import Path
import random
from PIL import Image, ImageDraw
ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/coldpenguins'
PALETTES = {
    'emperor': ((29,28,32), (237,229,212), (31,30,33), (35,33,35)),
    'gentoo': ((33,32,37), (237,235,235), (239,121,29), (232,124,32)),
    'african': ((33,31,35), (237,231,226), (36,34,37), (38,35,38)),
    'blue': ((51,74,106), (231,233,235), (39,47,59), (206,146,147)),
    'macaroni': ((32,31,37), (239,232,218), (226,111,28), (220,133,54)),
    'rockhopper': ((38,38,45), (239,232,218), (195,70,31), (213,127,62)),
}
DOWN = {'emperor':((169,168,175),(204,202,209),(29,28,32)),
        'gentoo':((120,110,111),(190,182,183),(89,81,83)),
        'african':((94,91,97),(167,164,171),(61,58,64)),
        'blue':((135,124,123),(194,182,177),(114,104,105)),
        'macaroni':((119,116,123),(193,187,186),(82,79,85)),
        'rockhopper':((113,111,119),(185,182,184),(76,73,80))}
CREST = {'macaroni': (241,162,30), 'rockhopper': (244,207,47)}

def cube(u,v,w,h,d):
    return {'front':(u+d,v+d,w,h),'right':(u,v+d,d,h),
            'left':(u+d+w,v+d,d,h),'back':(u+2*d+w,v+d,w,h),
            'top':(u+d,v,w,d),'bottom':(u+d+w,v,w,d)}

def paint(image,rect,color,rng,variation=6):
    x,y,w,h=rect
    for yy in range(y,y+h):
        for xx in range(x,x+w):
            n=rng.randint(-variation,variation)
            image.putpixel((xx,yy),tuple(max(0,min(255,c+n)) for c in color)+(255,))

def region(rect,x,y,w,h): return rect[0]+x,rect[1]+y,w,h

for species,(back,belly,bill,feet) in PALETTES.items():
    for baby in (False,True):
        rng=random.Random(species+str(baby))
        image=Image.new('RGBA',(64,64),(0,0,0,0))
        body=cube(0,0,9,8,7) if baby else cube(0,0,7,9,5)
        head=cube(0,18,8,7,7) if baby else cube(0,16,8,6,7) if species=='emperor' else cube(0,16,7,6,6)
        beak=cube(32,20,2,2,2) if baby else cube(32,18,3,2,3)
        flipper=cube(32,0,1,6 if baby else 8,4)
        foot=cube(44,0,3,1,4)
        if baby:
            coat,underside,cap=DOWN[species]
            for r in body.values(): paint(image,r,coat,rng,9)
            paint(image,body['front'],underside,rng,9)
            for r in flipper.values(): paint(image,r,coat,rng,9)
            for r in head.values(): paint(image,r,cap,rng,7)
            face=head['front']
            cheek=(226,224,222) if species=='emperor' else (204,197,195)
            if species=='african':cheek=(174,170,175)
            if species=='blue':cheek=(202,190,184)
            # Tall pale cheek panels, dark cap and narrow dark center above the bill.
            paint(image,region(face,0,2,3,5),cheek,rng)
            paint(image,region(face,5,2,3,5),cheek,rng)
            paint(image,region(face,1,1,2,1),cheek,rng)
            paint(image,region(face,5,1,2,1),cheek,rng)
            for side in ('left','right'):
                r=head[side];paint(image,region(r,0,3,r[2],4),cheek,rng)
            eye=(244,242,239,255)
            draw=ImageDraw.Draw(image)
            x,y,_,_=face
            draw.rectangle((x+1,y+3,x+1,y+4),fill=eye)
            draw.rectangle((x+6,y+3,x+6,y+4),fill=eye)
            for r in beak.values():paint(image,r,bill if species in CREST else (35,34,37),rng,3)
            for r in foot.values():paint(image,r,feet if species in CREST else (35,33,36),rng,3)
        else:
            for r in body.values():paint(image,r,back,rng)
            paint(image,body['front'],belly,rng,7)
            for side in ('left','right'):
                r=body[side]
                paint(image,region(r,0 if side=='left' else r[2]-1,0,1,r[3]),belly,rng)
            for r in head.values():paint(image,r,back,rng)
            for r in flipper.values():paint(image,r,back,rng)
            for r in foot.values():paint(image,r,feet,rng,5)
            for r in beak.values():paint(image,r,bill,rng,5)
            face=head['front'];x,y,w,h=face
            draw=ImageDraw.Draw(image)
            # White eye patches on dark faces, as in the adult concept art.
            draw.rectangle((x+1,y+2,x+1,y+3),fill=(245,243,242,255))
            draw.rectangle((x+5,y+2,x+5,y+3),fill=(245,243,242,255))
            if species=='emperor':
                front=body['front']
                for row,color in enumerate(((240,187,47),(240,204,104),(239,220,162))):
                    paint(image,region(front,0,row,front[2],1),color,rng,5)
                for side in ('left','right'):
                    r=head[side]
                    paint(image,region(r,2,2,3,2),(238,151,24),rng,5)
                    paint(image,region(r,1,4,4,2),(243,193,64),rng,5)
                    r=body[side];paint(image,region(r,0,0,r[2],2),(238,196,91),rng)
                for side,r in beak.items():
                    if side not in ('top','bottom'):
                        paint(image,region(r,0,1,r[2],1),(236,126,23),rng,4)
            elif species=='gentoo':
                # Broad white crown straps wrap onto both sides of the head.
                r=head['top']
                paint(image,region(r,0,0,2,r[3]),belly,rng,4)
                paint(image,region(r,5,0,2,r[3]),belly,rng,4)
                for side in ('left','right'):
                    r=head[side];paint(image,region(r,0,0,4,2),belly,rng,4)
                paint(image,region(face,0,0,2,1),belly,rng,4)
                paint(image,region(face,5,0,2,1),belly,rng,4)
            elif species=='african':
                # White face ring, pink brows, and an arched breast band.
                for side in ('left','right'):
                    r=head[side]
                    paint(image,region(r,2,1,2,5),belly,rng)
                    paint(image,region(r,0,4,4,2),belly,rng)
                paint(image,region(face,0,1,1,5),belly,rng)
                paint(image,region(face,6,1,1,5),belly,rng)
                paint(image,region(face,0,5,7,1),belly,rng)
                for xx in (0,5):paint(image,region(face,xx,0,2,1),(233,164,172),rng,4)
                r=body['front'];bx,by,_,_=r
                draw=ImageDraw.Draw(image)
                draw.line([(bx,by+4),(bx+1,by+2),(bx+2,by+1),(bx+4,by+1),(bx+5,by+2),(bx+6,by+4)],fill=(*back,255),width=1)
                for xx,yy in ((2,5),(4,6),(3,8)):draw.point((bx+xx,by+yy),fill=(*back,255))
        suffix='_chick' if baby else ''
        if not baby and species in ('emperor','african'):
            # Two texels per model pixel make the concept's eye, face-ring and collar
            # shapes readable without increasing geometry complexity.
            image=image.resize((128,128),Image.Resampling.NEAREST)
            def hi(rect): return tuple(value*2 for value in rect)
            def fill(rect,color,variation=5): paint(image,rect,color,rng,variation)
            face=hi(head['front']);fx,fy,fw,fh=face
            for r in head.values():fill(hi(r),back)
            if species=='emperor':
                for side in ('left','right'):
                    sx,sy,sw,sh=hi(head[side])
                    fill((sx+8,sy+3,6,5),(239,149,22))
                    fill((sx+6,sy+7,8,3),(246,180,34))
                    fill((sx+3,sy+10,11,2),(246,206,100))
                # Small gold corners join the large cheek patches to the collar.
                for xx in (0,fw-1):fill((fx+xx,fy+10,1,2),(244,202,92))
                bx,by,bw,bh=hi(body['front'])
                for row,color in enumerate(((242,183,38),(246,196,61),(247,211,111),(245,222,155),(242,226,183))):
                    fill((bx,by+row,bw,1),color)
                # A black-tipped bill with orange lower side panels.
                for side,r in beak.items():
                    fill(hi(r),bill,3)
                    if side in ('left','right'):
                        qx,qy,qw,qh=hi(r);fill((qx,qy+qh-2,qw-1,2),(239,133,23),3)
                eyeXs=(2,fw-4)
            else:
                # Pale ring wraps behind the dark face, meeting under the chin.
                draw=ImageDraw.Draw(image)
                for side in ('left','right'):
                    sx,sy,sw,sh=hi(head[side])
                    polygon=[(sx+5,sy+1),(sx+9,sy+3),(sx+11,sy+6),(sx+9,sy+10),
                             (sx+5,sy+11),(sx,sy+11),(sx,sy+9),(sx+5,sy+8),(sx+7,sy+6),(sx+5,sy+4),(sx+3,sy+3)]
                    draw.polygon(polygon,fill=(*belly,255))
                fill((fx,fy+4,2,7),belly);fill((fx+fw-2,fy+4,2,7),belly)
                fill((fx,fy+10,fw,2),belly)
                fill((fx,fy+1,4,3),(233,165,175));fill((fx+fw-4,fy+1,4,3),(233,165,175))
                # A continuous arched breast band, separate from the small belly spots.
                bx,by,bw,bh=hi(body['front']);fill((bx,by,bw,bh),belly,7)
                draw=ImageDraw.Draw(image)
                draw.line([(bx,by+8),(bx+2,by+5),(bx+4,by+3),(bx+9,by+3),(bx+11,by+5),(bx+13,by+8)],fill=(*back,255),width=2)
                for xx,yy in ((4,12),(9,14),(6,17)):draw.rectangle((bx+xx,by+yy,bx+xx+1,by+yy+1),fill=(*back,255))
                eyeXs=(3,fw-5)
            draw=ImageDraw.Draw(image)
            for ex in eyeXs:
                draw.rectangle((fx+ex,fy+5,fx+ex+2,fy+7),fill=(246,244,243,255))
                # Pupils are rendered separately so their gaze can move.
        if not baby and species in CREST:
            crest = CREST[species]
            if species == 'macaroni':
                # Brow design: a continuous forehead band with two short swept tips.
                for r in cube(0,40,7,1,1).values(): paint(image,r,crest,rng)
                for r in cube(18,40,2,1,3).values(): paint(image,r,crest,rng)
                paint(image,region(face,0,0,7,1),crest,rng)
            else:
                for r in cube(0,40,3,1,1).values(): paint(image,r,crest,rng)
                for r in cube(12,40,1,1,2).values(): paint(image,r,back,rng)
                for xx in (0,5): paint(image,region(face,xx,1,2,1),crest,rng)
            # Leave a gap below the crest and room for the animated dark pupil.
            paint(image,region(face,0,2,7,2),back,rng)
            draw=ImageDraw.Draw(image)
            eye_color=(194,54,51,255) if species == 'rockhopper' else (245,243,242,255)
            for eye_x in (1,5):
                draw.rectangle((x+eye_x,y+2,x+eye_x,y+3),fill=eye_color)
        image.save(ROOT/'textures/entity'/f'penguin_{species}{suffix}.png')
    # A native 16px egg silhouette with species colors and contrasting speckles.
    egg=Image.new('RGBA',(16,16),(0,0,0,0));draw=ImageDraw.Draw(egg)
    for yy in range(2,15):
        half=[1,2,3,4,5,5,6,6,6,5,5,4,2][yy-2]
        shade=(0.72+0.22*(1-(yy-2)/13))
        color=tuple(int(c*shade) for c in back)+(255,)
        draw.line((7-half,yy,8+half,yy),fill=color)
    accents={'emperor':(244,188,49),'gentoo':(240,130,36),'african':(233,169,177),'blue':(213,153,153), **CREST}
    for px,py in ((5,5),(10,7),(4,10),(9,12)):
        draw.rectangle((px,py,px+1,py+1),fill=(*accents[species],255))
    draw.line((5,3,7,3),fill=(*belly,255))
    egg.save(ROOT/'textures/item'/f'{species}_penguin_spawn_egg.png')
