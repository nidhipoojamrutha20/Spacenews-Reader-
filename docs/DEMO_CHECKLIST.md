# SpaceNews demo checklist

Use an emulator/device with internet access for live network features. Existing Room bookmarks are stored locally; avoid clearing app storage when demonstrating persistence.

1. Launch **SpaceNews** and wait for the Home feed. Confirm article images, publisher, and publication date appear. Pull down to refresh.
2. Scroll to the bottom of a feed to trigger API pagination. Retry the footer if a page request fails.
3. Open **Explore**. Select **NASA** or **SpaceX**, then enter a keyword. Pause briefly for debouncing; confirm results update. Clear the text to restore the selected topic feed. Pull down to refresh.
4. Tap a result. Confirm the detail view shows the image when available, title, publisher, date, and a labeled API summary. The original publisher article is a separate action.
5. Save the article with the bookmark control. Return to another list and confirm the bookmark remains selected. Toggle it again and confirm it is removed.
6. Open **For You**. If it is empty, open **Settings**, select interests such as NASA, SpaceX, Mars, or ISRO, then return. Pull to refresh and observe results assembled from interest searches.
7. Open a For You or Explore story. Set reading size to Small/Medium/Large in Settings and verify the detail title and summary resize. In details, choose Slow/Normal/Fast, press **Play summary**, then **Stop**.
8. Open **Saved**. Search part of a saved title and try Recently saved, Oldest saved, and Title A–Z. Remove the saved item and verify it disappears.
9. With a saved item, enable Airplane mode and open its detail from **Saved**. Verify stored text and metadata remain. The image and external publisher link may not be available offline.
10. Force-stop and reopen the app without clearing data. Verify the bookmark and preferences persist.
11. For error handling, temporarily disable network, try refreshing Home or searching Explore, confirm an error and Retry action, then restore network.
