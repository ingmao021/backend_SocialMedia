curl -s -X POST "https://backend-socialmedia-ixsm.onrender.com/api/videos/generate" \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJvcmRvbmV6bTAyMUBnbWFpbC5jb20iLCJ1c2VySWQiOjEsImlhdCI6MTc3NTY2NDc3MSwiZXhwIjoxNzc1NzUxMTcxfQ.uqc4dVCqjywq8pFDgaLezfVo6pn5lIxvgRds6fE_Yt1GwVBbM2bkQvldzZ_4k-wxrxOjYSnRhfTQH6rrFwLHzQ" \
  -H "Content-Type: application/json" \
  -d '{"title": "Test", "description": "Test Veo 3", "prompt": "A red ball rolling on grass"}'