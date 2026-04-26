curl -s -X POST "https://backend-socialmedia-ixsm.onrender.com/api/videos/generate" \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJvcmRvbmV6bTAyMUBnbWFpbC5jb20iLCJ1c2VySWQiOjEsImlhdCI6MTc3NTY2NjAyNCwiZXhwIjoxNzc1NzUyNDI0fQ.E3Jo5N9WSZdA_LPHpsHAuBuUyp7jLayxNxCMpfb2rUZw_ap_7CJtuiICjfFUm1SsN6BAdFFfQgMk660lbWkqGA" \
  -H "Content-Type: application/json" \
  -d '{"title": "Test", "description": "Test Veo 3", "prompt": "A red ball rolling on grass"}'