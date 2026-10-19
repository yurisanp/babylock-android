import math
def f(x): return f"{x:.2f}".rstrip('0').rstrip('.')
def lock(cx=54, cy=54, w=40, h=44):
    L, T = cx - w/2, cy - h/2
    s = w*0.11; bodyTop = T + h*0.42; arcW = w*0.56
    aL = L + (w-arcW)/2; aT = T + s/2; legB = bodyTop + s; r = arcW/2
    shackle = f"M{f(aL)},{f(legB)}L{f(aL)},{f(aT+r)}A{f(r)},{f(r)} 0 0 1 {f(aL+arcW)},{f(aT+r)}L{f(aL+arcW)},{f(legB)}"
    rr = w*0.24; R, B = L+w, T+h
    body = (f"M{f(L+rr)},{f(bodyTop)}H{f(R-rr)}A{f(rr)},{f(rr)} 0 0 1 {f(R)},{f(bodyTop+rr)}V{f(B-rr)}"
            f"A{f(rr)},{f(rr)} 0 0 1 {f(R-rr)},{f(B)}H{f(L+rr)}A{f(rr)},{f(rr)} 0 0 1 {f(L)},{f(B-rr)}V{f(bodyTop+rr)}A{f(rr)},{f(rr)} 0 0 1 {f(L+rr)},{f(bodyTop)}Z")
    c = (cx, bodyTop + (B-bodyTop)*0.52); r1 = w*0.15
    c2 = (c[0]+r1*0.55, c[1]-r1*0.35); r2 = r1*0.9
    dx, dy = c2[0]-c[0], c2[1]-c[1]; d = math.hypot(dx, dy)
    a = (r1*r1 - r2*r2 + d*d)/(2*d); hh = math.sqrt(r1*r1 - a*a)
    mx, my = c[0]+a*dx/d, c[1]+a*dy/d
    p1 = (mx + hh*dy/d, my - hh*dx/d); p2 = (mx - hh*dy/d, my + hh*dx/d)
    # outer arc on C1 from p1 to p2 the long way (away from bite), inner arc on C2 back
    moon = (f"M{f(p1[0])},{f(p1[1])}A{f(r1)},{f(r1)} 0 1 0 {f(p2[0])},{f(p2[1])}"
            f"A{f(r2)},{f(r2)} 0 0 1 {f(p1[0])},{f(p1[1])}Z")
    return dict(shackle=shackle, stroke=s, body=body, moon=moon)
def sparkle(x, y, r):
    k = r*0.22
    return (f"M{f(x)},{f(y-r)}Q{f(x+k)},{f(y-k)} {f(x+r)},{f(y)}Q{f(x+k)},{f(y+k)} {f(x)},{f(y+r)}"
            f"Q{f(x-k)},{f(y+k)} {f(x-r)},{f(y)}Q{f(x-k)},{f(y-k)} {f(x)},{f(y-r)}Z")
