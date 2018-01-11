import cv2
import numpy as np
import matplotlib.pyplot as plt
import socket
import sys

s = None
if len(sys.argv) > 1:
    TCP_IP = sys.argv
    TCP_PORT = 9000
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect((TCP_IP, TCP_PORT))
    cap = cv2.VideoCapture(sys.argv)
    delay = 1
else:
    cap = cv2.VideoCapture("taxi.mp4")
    delay = 0

prev_roi = []

def roiDist(r1, r2):
    d = 0
    for i in range(3):
        for j in range(3):
            d += abs(r1['pixels'][i, j] - r2['pixels'][i, j])
    return d

def extractRoi(gray):
    roi = [];
    h, w = gray.shape[:2]

    # Only look for corners in left and right extremities of the frame
    lh = gray[int(20/100*h):int(80/100*h), int(5/100*w):int(45/100*w)]
    rh = gray[int(20/100*h):int(80/100*h), int(55/100*w):int(95/100*w)]
    lh_corners = cv2.goodFeaturesToTrack(lh, 16, 0.05, 5)
    lh_corners = np.float32(lh_corners)

    for item in lh_corners:
        x, y = item[0]
        x += 5/100*w
        y += 20/100*h
        x = int(x)
        y = int(y)
        roi += [{
            'center': (x, y),
            'pixels': gray[y-3:y+3, x-3:x+3]
        }]

    rh_corners = cv2.goodFeaturesToTrack(rh, 16, 0.05, 5)
    rh_corners = np.float32(rh_corners)

    for item in rh_corners:
        x, y = item[0]
        x += 55/100*w
        y += 20/100*h
        x = int(x)
        y = int(y)
        roi += [{
            'center': (x, y),
            'pixels': gray[y-3:y+3, x-3:x+3]
        }]

    return roi

while(True):
    ret, frame = cap.read()
    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)

    # Get an array where each element is a corner`s center and its surrounding pixels
    roi = extractRoi(gray.copy())

    # Match each corner from previous frame with one from the new frame
    if len(prev_roi) > 0:
        dx = 0;
        dy = 0;
        matches = 0
        for i in range(len(prev_roi)):
            min_val = 9999
            min_idx = -1
            for j in range(len(roi)):
                d = roiDist(prev_roi[i], roi[j])
                if d < min_val:
                    min_val = d
                    min_idx = j

            # plt.subplot(2, 8, i + 1)
            # plt.imshow(roi[min_idx]['pixels'], cmap='gray')
            # plt.title(str(min_idx) + " roi " + str(min_val))
            # plt.subplot(2, 8, i + 9)
            # plt.imshow(prev_roi[i]['pixels'], cmap='gray')
            # plt.title(str(i) + " prev_roi")

            if min_val < 800:
                matches += 1
                roix, roiy = roi[min_idx]['center']
                prev_roix, prev_roiy = prev_roi[i]['center']
                dx += (prev_roix - roix)
                dy += (prev_roiy - roiy)

        # plt.show()

        # Print result to console and on TCP socket if connected
        print(round(dx/matches), round(dy/matches))
        if s != None:
            s.send(str(round(dx/matches)) + " " + str(round(dy/matches)))

    prev_roi = roi

    # Display the current frame
    cv2.imshow('frame',gray)
    if cv2.waitKey(delay) & 0xFF == ord('q'):
        break

# When everything done, release the capture
cap.release()
cv2.destroyAllWindows()
